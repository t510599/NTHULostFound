package ss.team16.nthulostfound.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
    var fabExtended by remember { mutableStateOf(true) }

    LaunchedEffect(viewModel.lazyListState) {
        var prevOffset = 0
        var prevIndex = 0
        snapshotFlow { viewModel.lazyListState.firstVisibleItemScrollOffset }
            .collect {
                fabExtended =
                    (viewModel.lazyListState.firstVisibleItemIndex == prevIndex &&
                            viewModel.lazyListState.firstVisibleItemScrollOffset <= prevOffset) ||
                            viewModel.lazyListState.firstVisibleItemIndex < prevIndex

                prevOffset = viewModel.lazyListState.firstVisibleItemScrollOffset
                prevIndex = viewModel.lazyListState.firstVisibleItemIndex
            }
    }


    Scaffold(
        topBar = {
            val avatar = viewModel.avatarBitmap?.collectAsState()?.value
            
            HomeAppBar(
                navigateToRoute = { navController.navigate(it) },
                onSearch = { viewModel.onSearch(it) },
                avatar = avatar,
                isMyItems = viewModel.myItemsFlow.collectAsState().value,
                onMyItemsChanged = { viewModel.myItemsFlow.value = it },
                title =
                    if (showType == ShowType.FOUND)
                        "拾獲物品"
                    else
                        "協尋物品"
            )
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
                    labelTextColor = MaterialTheme.colors.onSecondary,
                    labelBackgroundColor = MaterialTheme.colors.secondary,
                    fabBackgroundColor = MaterialTheme.colors.secondary,
                    onClick = { navController.navigate("new_item/found") }
                ),
                MultiFabItem(
                    icon = Icons.Filled.Help,
                    label = "我東西掉了",
                    labelTextColor = MaterialTheme.colors.onSecondary,
                    labelBackgroundColor = MaterialTheme.colors.secondary,
                    fabBackgroundColor = MaterialTheme.colors.secondary,
                    onClick = { navController.navigate("new_item/lost") }
                )
            )
            MultiFloatingActionButton(
                srcIcon = Icons.Filled.Add,
                fabBackgroundColor = MaterialTheme.colors.secondary,
                items = expandFabItemList,
                wideButton = fabExtended,
                wideButtonLabel = "新增"
            )
        },
        bottomBar = {
            BottomNav(
                currentShowType = showType,
                modifier = modifier,
                onChangePage = {
                    viewModel.onPageChanged(it)
                }
            )
        }
    ) { paddingValues ->

        Column(
            modifier = modifier
                .padding(paddingValues)
        ) {
            // default: hide both before flow updated
            val showPinMessage = viewModel.showPinMessageFlow.collectAsState(initial = 0b00)
            val canShowPopUp = viewModel.canShowPopUpFlow.collectAsState(initial = false)
            val isMyItems = viewModel.myItemsFlow.collectAsState().value
            val search = viewModel.searchFlow.collectAsState().value

            if (showType == ShowType.FOUND) {
                AnimatedVisibility(
                    // check FOUND bit
                    visible = (showPinMessage.value and 0b10 != 0) && !isMyItems,
                    exit = slideOutVertically(
                        targetOffsetY = { -it },
                    ) + shrinkVertically()
                ) {
                    PinMessage(
                        message = "其他人撿到的遺失物將會刊登在這裡\n" +
                                "如果您遺失了物品，請從這裡尋找！",
                        onClose = { viewModel.onPinMessageClose() }
                    )
                }
            } else if (showType == ShowType.LOST) {
                AnimatedVisibility(
                    // check LOST bit
                    visible = (showPinMessage.value and 0b01 != 0) && !isMyItems,
                    exit = slideOutVertically(
                        targetOffsetY = { -it }
                    ) + shrinkVertically()
                ) {
                    PinMessage(
                        message = "這裡是失主刊登遺失物的地方\n" +
                                "如果您有發現這些物品，請與失主聯絡！",
                        onClose = { viewModel.onPinMessageClose() }
                    )
                }
            }
            AnimatedVisibility(
                visible = isMyItems,
                exit = slideOutVertically(
                    targetOffsetY = { -it },
                ) + shrinkVertically()
            ) {
                PinMessage(
                    message = "您正在瀏覽您的發佈記錄",
                    onClose = { viewModel.myItemsFlow.value = false }
                )
            }

            val lazyPagingItems = viewModel.items.collectAsLazyPagingItems()

            val isRefreshing = lazyPagingItems.loadState.refresh == LoadState.Loading
            val swipeRefreshState = rememberSwipeRefreshState(isRefreshing)

            SwipeRefresh(
                state = swipeRefreshState,
                onRefresh = { lazyPagingItems.refresh() },
                modifier = Modifier.fillMaxSize()
            ) {
                if (lazyPagingItems.loadState.refresh is LoadState.Error) {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = "No Internet Connection",
                            modifier = Modifier
                                    .size(150.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "無網際網路連線，請連接 Wi-Fi 或是開啟行動數據。",
                            style = MaterialTheme.typography.body2
                        )
                    }
                } else if (lazyPagingItems.itemCount == 0 &&
                    lazyPagingItems.loadState.append.endOfPaginationReached) {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = "No Result",
                            modifier = Modifier
                                .size(150.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text =
                                if (search != null)
                                    "沒有找到結果！請試試以其他關鍵字搜尋"
                                else
                                    "沒有任何發佈記錄！"
                            ,
                            style = MaterialTheme.typography.body2
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        state = viewModel.lazyListState
                    ) {

                        if (lazyPagingItems.loadState.refresh == LoadState.Loading) {
                            items(10) {
                                ItemCardShimmer()
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
            }

            val isScrolledToEnd = remember(viewModel.lazyListState) {
                derivedStateOf {
                    viewModel.lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ==
                            viewModel.lazyListState.layoutInfo.totalItemsCount - 1
                }
            }
            if (isScrolledToEnd.value
                && lazyPagingItems.loadState.append.endOfPaginationReached
                && lazyPagingItems.loadState.append != LoadState.Loading
                && !isMyItems
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
            else if(canShowPopUp.value) {
                LaunchedEffect(Unit) {
                    launch {
                        val snackbarResult = scaffoldState.snackbarHostState.showSnackbar(
                            message = "您似乎尚未填寫個人資料，立即填寫以便在遺失物品時收到通知！",
                            actionLabel = "前往填寫"
                        )
                        when (snackbarResult) {
                            SnackbarResult.ActionPerformed -> {
                                navController.navigate("profile")
                            }
                            SnackbarResult.Dismissed -> {}
                        }
                        viewModel.onSetUserDataPopUp()
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