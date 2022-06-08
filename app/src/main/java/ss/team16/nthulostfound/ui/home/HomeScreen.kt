package ss.team16.nthulostfound.ui.home

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemsIndexed
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.valentinilk.shimmer.shimmer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ss.team16.nthulostfound.domain.model.ItemData
import ss.team16.nthulostfound.ui.components.*

@Composable
fun HomeScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val scaffoldState = rememberScaffoldState()
    val showType = viewModel.showTypeFlow.collectAsState().value

    Scaffold(
        topBar = {
            HomeAppBar(navigateToRoute = {
                navController.navigate(it)
            }, onSearch = {
                viewModel.onSearch(it)
            })
        },
        scaffoldState = scaffoldState,
        snackbarHost = {
            SnackbarHost(it) { data ->
                Snackbar(
                    snackbarData = data,
//                    actionOnNewLine = true
                )
            }
        },
        floatingActionButton = {
            val expandFabItemList: MutableList<MultiFabItem> = mutableListOf(
                MultiFabItem(
                    icon = Icons.Filled.Search,
                    label = "我撿到東西了",
                    fabBackgroundColor = Color.Black,
                    onClick = { navController.navigate("new_item/found") }
                ),
                MultiFabItem(
                    icon = Icons.Filled.Help,
                    label = "我東西掉了",
                    fabBackgroundColor = Color.Black,
                    onClick = { navController.navigate("new_item/lost") }
                )
            )
            MultiFloatingActionButton(
                srcIcon = Icons.Filled.Add,
                items = expandFabItemList)
        },
        bottomBar = {
            BottomNav(
                currentShowType = showType,
                modifier = modifier,
                onChangePage = { viewModel.onPageChanged(it) }
            )
        }
    ) { paddingValues ->

        val lazyState = rememberLazyListState()
        val lazyPagingItems = viewModel.items.collectAsLazyPagingItems()

        val isRefreshing = lazyPagingItems.loadState.refresh == LoadState.Loading
        val swipeRefreshState = rememberSwipeRefreshState(isRefreshing)

        SwipeRefresh(
            state = swipeRefreshState,
            onRefresh = { lazyPagingItems.refresh() },
            modifier = Modifier.padding(paddingValues)
        ) {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                state = lazyState
            ) {

                if (lazyPagingItems.loadState.refresh == LoadState.Loading) {
                    items(10) {
                        ItemCard(
                            item = ItemData(
                                name = "...",
                                place = "...",
                            ),
                            modifier = Modifier.shimmer(),
                            onClick = {}
                        )
                    }
                }

                itemsIndexed(lazyPagingItems) { _, item ->
                    if (item != null) {
                        ItemCard(item = item, onClick = {
                            navController.navigate("item/${item.uuid}")
                        })
                    }
                }

                if (lazyPagingItems.loadState.append == LoadState.Loading) {
                    item {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentWidth(Alignment.CenterHorizontally)
                        )
                    }
                }
            }
        }

        val isScrolledToEnd = remember(lazyState) {
            derivedStateOf {
                lazyState.layoutInfo.visibleItemsInfo.lastOrNull()?.index == lazyState.layoutInfo.totalItemsCount - 1
            }
        }
        if (isScrolledToEnd.value
            && lazyPagingItems.loadState.append.endOfPaginationReached
            && lazyPagingItems.loadState.append != LoadState.Loading
            && showType == ShowType.FOUND) {
            LaunchedEffect(Unit) {
                launch {
                    val snackbarResult = scaffoldState.snackbarHostState.showSnackbar(
                        message = "沒有找到您的失物？不要氣餒！立即新增協尋物品，讓找到的人能立刻聯繫到您！",
                        actionLabel = "前往新增"
                    )
                    when (snackbarResult) {
                        SnackbarResult.ActionPerformed -> {
                            navController.navigate("new_item/lost")
                        }
                        SnackbarResult.Dismissed -> {}
                    }
                }
            }
        }
    }
}


//@Preview
//@Composable
//fun HomeScreenPreview() {
//    NTHULostFoundTheme {
//        HomeScreen(null)
//    }
//}