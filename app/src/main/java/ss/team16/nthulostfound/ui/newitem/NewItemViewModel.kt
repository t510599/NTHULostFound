package ss.team16.nthulostfound.ui.newitem

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.runtime.*
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.PagerState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ss.team16.nthulostfound.domain.model.NewItemType
import ss.team16.nthulostfound.domain.usecase.UploadImagesUseCase
import java.util.*
import javax.inject.Inject

@HiltViewModel
class NewItemViewModel @Inject constructor(
    state: SavedStateHandle,
    val uploadImagesUseCase: UploadImagesUseCase
) : ViewModel() {

    private val newItemType = state.get<String>("new_item_type")!!

    val type =
        if (newItemType == "found")
            NewItemType.NEW_FOUND
        else
            NewItemType.NEW_LOST

    @OptIn(ExperimentalPagerApi::class)
    var pagerState by mutableStateOf(PagerState(0))
        private set

    @OptIn(ExperimentalPagerApi::class)
    fun getPagerPrevButtonInfo(): PagerButtonInfo? {
        return when (NewItemPageInfo.fromInt(pagerState.currentPage)) {
            NewItemPageInfo.EDIT -> null
            NewItemPageInfo.CONFIRM -> PagerButtonInfo("返回編輯", true)
            NewItemPageInfo.SENDING -> null
            NewItemPageInfo.DONE -> null
        }
    }

    @OptIn(ExperimentalPagerApi::class)
    fun getPagerNextButtonInfo(): PagerButtonInfo? {
        return when (NewItemPageInfo.fromInt(pagerState.currentPage)) {
            NewItemPageInfo.EDIT -> PagerButtonInfo("確認資訊", true)
            NewItemPageInfo.CONFIRM -> PagerButtonInfo("確定送出", true)
            NewItemPageInfo.SENDING -> PagerButtonInfo("完成", false)
            NewItemPageInfo.DONE -> PagerButtonInfo("完成", true)
        }
    }

    @OptIn(ExperimentalPagerApi::class)
    fun goToNextPage(scrollToPage: (Int) -> Unit, popScreen: () -> Unit) {
        if (pagerState.currentPage == NewItemPageInfo.DONE.value) {
            popScreen()
        } else {
            val curPage = pagerState.currentPage
            val nextPage = curPage + 1
            if (pagerState.isScrollInProgress || nextPage >= pagerState.pageCount)
                return

            when (curPage) {
                NewItemPageInfo.EDIT.value -> {
                    showFieldErrors = true
                    if (!validateFields())
                        return
                }
                NewItemPageInfo.CONFIRM.value -> {
                    doWork { scrollToPage(NewItemPageInfo.DONE.value) }
                }
                else -> {}
            }

            scrollToPage(nextPage)
        }
    }

    @OptIn(ExperimentalPagerApi::class)
    fun goToPrevPage(scrollToPage: (Int) -> Unit) {
        val pagePrev = pagerState.currentPage - 1
        if (pagerState.isScrollInProgress || pagePrev < 0)
            return

        scrollToPage(pagePrev)
    }

    private fun doWork(doneCallback: () -> Unit) {
        viewModelScope.launch {
            uploadImagesUseCase()
            doneCallback()
        }
    }

    private val _imageBitmaps = emptyList<Bitmap>().toMutableStateList()
    val imageBitmaps: List<Bitmap>
        get() = _imageBitmaps

    fun onAddImage(uri: Uri?, context: Context) {
        if (uri == null)
            return

        if (Build.VERSION.SDK_INT < 28) {
            _imageBitmaps.add(
                MediaStore.Images.Media.getBitmap(
                    context.contentResolver,
                    uri
                )
            )
        } else {
            val source = ImageDecoder
                .createSource(context.contentResolver, uri)
            _imageBitmaps.add(ImageDecoder.decodeBitmap(source))
        }
    }

    fun onDeleteImage(index: Int) {
        _imageBitmaps.removeAt(index)
    }

    private val calendar = Calendar.getInstance()

    var year by mutableStateOf(calendar[Calendar.YEAR])
        private set
    var month by mutableStateOf(calendar[Calendar.MONTH])
        private set
    var day by mutableStateOf(calendar[Calendar.DAY_OF_MONTH])
        private set
    var hour by mutableStateOf(calendar[Calendar.HOUR_OF_DAY])
        private set
    var minute by mutableStateOf(calendar[Calendar.MINUTE])
        private set

    fun onDateChange(year: Int, month: Int, day: Int) {
        this.year = year
        this.month = month
        this.day = day
    }
    fun onTimeChange(hour: Int, minute: Int) {
        this.hour = hour
        this.minute = minute
    }

    var name by mutableStateOf("")
        private set
    fun onNameChange(value: String) {
        name = value
    }

    var place by mutableStateOf("")
        private set
    fun onPlaceChange(value: String) {
        place = value
    }

    var description by mutableStateOf("")
        private set
    fun onDescriptionChange(value: String) {
        description = value
    }

    var how by mutableStateOf("")
        private set
    fun onHowChange(value: String) {
        how = value
    }

    var contact by mutableStateOf("")
        private set
    fun onContactChange(value: String) {
        contact = value
    }

    var whoEnabled by mutableStateOf(type == NewItemType.NEW_FOUND)
        private set
    fun onWhoEnabledChange(value: Boolean) {
        whoEnabled = value
    }

    var who by mutableStateOf("")
        private set
    fun onWhoChange(value: String) {
        who = value
    }

    var showFieldErrors by mutableStateOf(false)
        private set

    private fun validateFields(): Boolean {
        return name.isNotBlank() &&
                place.isNotBlank() &&
                how.isNotBlank() &&
                contact.isNotBlank() &&
                !(whoEnabled && who.isBlank())
    }
}

enum class NewItemPageInfo(val value: Int) {
    EDIT(0),
    CONFIRM(1),
    SENDING(2),
    DONE(3);

    companion object {
        fun fromInt(value: Int) = values().first { it.value == value }
    }
}

data class PagerButtonInfo(
    val label: String,
    val enabled: Boolean
)