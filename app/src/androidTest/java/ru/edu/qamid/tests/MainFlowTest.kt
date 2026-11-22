package ru.edu.qamid.tests

import io.appium.java_client.AppiumBy
import io.appium.java_client.android.AndroidDriver
import io.appium.java_client.android.options.UiAutomator2Options
import io.qameta.allure.android.runners.AllureAndroidJUnit4
import io.qameta.allure.kotlin.Allure
import io.qameta.allure.kotlin.Description
import io.qameta.allure.kotlin.Epic
import io.qameta.allure.kotlin.Feature
import io.qameta.allure.kotlin.Severity
import io.qameta.allure.kotlin.SeverityLevel
import io.qameta.allure.kotlin.Story
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.RunWith
import org.openqa.selenium.OutputType
import org.openqa.selenium.TakesScreenshot
import ru.edu.qamid.tests.pages.AboutScreen
import ru.edu.qamid.tests.pages.FilterScreen
import ru.edu.qamid.tests.pages.LoginPage
import ru.edu.qamid.tests.pages.MainPage
import ru.edu.qamid.tests.pages.NewsScreen
import java.io.ByteArrayInputStream
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@RunWith(AllureAndroidJUnit4::class)
@Epic("Мобильный хоспис")
@Feature("Основной функционал приложения")
class MainFlowTest {

    private lateinit var driver: AndroidDriver
    private lateinit var loginPage: LoginPage
    private lateinit var mainPage: MainPage
    private lateinit var newsScreen: NewsScreen
    private lateinit var filterScreen: FilterScreen
    private lateinit var aboutScreen: AboutScreen

    companion object {
        private const val APP_PACKAGE = "ru.edu.qamid"
        private val FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy")

        init {
            System.setProperty("allure.results.directory", "allure-results")
        }
    }

    @get:Rule
    val screenshotWatcher = object : TestWatcher() {
        override fun failed(e: Throwable, description: org.junit.runner.Description) {
            if (!::driver.isInitialized) return
            try {
                val screenshot = (driver as TakesScreenshot).getScreenshotAs(OutputType.BYTES)
                Allure.attachment(
                    "Скриншот при падении: ${description.methodName}",
                    ByteArrayInputStream(screenshot),
                    "image/png",
                    "png"
                )
                Files.write(Paths.get("allure-results", "screenshot-${description.methodName}.png"), screenshot)
            } catch (err: Exception) {
            }
        }
    }

    @Before
    fun setUp() {
        val options = UiAutomator2Options()
        driver = AndroidDriver(URL("http://10.0.2.2:4723"), options)
        Allure.step("Запуск приложения") {
            Thread.sleep(1000)
            driver.activateApp(APP_PACKAGE)
            Thread.sleep(3000)
        }
        loginPage = LoginPage(driver)
        mainPage = MainPage(driver)
        newsScreen = NewsScreen(driver)
        filterScreen = FilterScreen(driver)
        aboutScreen = AboutScreen(driver)
        Allure.step("Подготовка: выйти, если уже выполнен вход") {
            if (mainPage.isMainScreen(5)) {
                mainPage.tapAuthorization()
                mainPage.tapLogout()
                Thread.sleep(2000)
            }
        }
        Allure.step("Авторизация при необходимости") {
            if (!mainPage.isMainScreen(5)) {
                assertTrue("Не удалось авторизоваться", loginPage.login())
            }
        }
    }

    @After
    fun tearDown() {
        if (::driver.isInitialized) {
            try {
                driver.quit()
            } catch (e: Exception) {
            }
        }
    }

    private fun ensureMainScreen() {
        Allure.step("Проверка главного экрана, повторная авторизация при необходимости") {
            var onMain = false
            val deadline = System.currentTimeMillis() + 10000
            while (System.currentTimeMillis() < deadline) {
                if (mainPage.isMainScreen()) {
                    onMain = true
                    break
                }
                Thread.sleep(1000)
            }
            if (onMain) return@step
            for (attempt in 1..4) {
                driver.navigate().back()
                Thread.sleep(1500)
                if (mainPage.isMainScreen(3)) return@step
            }
            assertTrue("Не удалось попасть на главный экран", loginPage.login())
            assertTrue("Главный экран не открылся после входа", mainPage.isMainScreen(15))
        }
    }

    private fun openAllNewsReliably() {
        Allure.step("Открыть ALL NEWS с ожиданием кнопки") {
            if (isNewsCardsVisible()) return@step
            val deadline = System.currentTimeMillis() + 15000
            while (System.currentTimeMillis() < deadline) {
                val btn = driver.findElements(AppiumBy.id("$APP_PACKAGE:id/all_news_text_view"))
                if (btn.isNotEmpty()) {
                    btn.first().click()
                    newsScreen.isScreenOpened()
                    return@step
                }
                Thread.sleep(1000)
            }
            assertTrue("Кнопка ALL NEWS не найдена на главном экране", false)
        }
    }

    private fun isNewsCardsVisible(): Boolean =
        driver.findElements(AppiumBy.id("$APP_PACKAGE:id/news_item_title_text_view")).isNotEmpty()

    private fun getVisibleNewsDates(): List<LocalDate> {
        val result = mutableListOf<LocalDate>()
        val cards = driver.findElements(AppiumBy.id("$APP_PACKAGE:id/news_item_material_card_view"))
        for (card in cards) {
            val texts = card.findElements(AppiumBy.className("android.widget.TextView"))
                .map { it.text }
            val pubIdx = texts.indexOfFirst { it.contains("Publication") }
            val dateText = if (pubIdx >= 0 && pubIdx + 1 < texts.size) texts[pubIdx + 1] else null
            val parsed = dateText?.let { runCatching { LocalDate.parse(it.trim(), FORMATTER) }.getOrNull() }
            val fallback = if (parsed == null) {
                texts.firstOrNull {
                    Regex("^\\d{2}\\.\\d{2}\\.\\d{4}$").matches(it.trim()) &&
                            runCatching { LocalDate.parse(it.trim(), FORMATTER) }.getOrDefault(LocalDate.MAX).year < 2100
                }?.let { runCatching { LocalDate.parse(it.trim(), FORMATTER) }.getOrNull() }
            } else null
            (parsed ?: fallback)?.let { result.add(it) }
        }
        return result
    }

    private fun confirmPickerDialog(): Boolean {
        Thread.sleep(1000)
        val okButton = driver.findElements(AppiumBy.id("android:id/button1"))
        return if (okButton.isNotEmpty()) {
            okButton.first().click()
            Thread.sleep(1000)
            true
        } else {
            false
        }
    }

    private fun selectCategory(name: String): Boolean {
        for (attempt in 1..3) {
            val field = driver.findElements(AppiumBy.id("$APP_PACKAGE:id/news_category_auto_complete"))
                .firstOrNull() ?: return false
            if (field.text.trim() == name) return true
            try {
                driver.hideKeyboard()
            } catch (e: Exception) {
            }
            Thread.sleep(500)
            try {
                field.click()
                Thread.sleep(500)
                field.clear()
                field.sendKeys(name)
                Thread.sleep(1000)
                if (field.text.trim() == name) {
                    try {
                        driver.hideKeyboard()
                    } catch (e: Exception) {
                    }
                    Thread.sleep(500)
                    val committed = runCatching {
                        driver.findElement(AppiumBy.id("$APP_PACKAGE:id/news_category_auto_complete")).text
                    }.getOrDefault("").trim()
                    if (committed == name) return true
                }
            } catch (t: Throwable) {
            }
            try {
                field.click()
                Thread.sleep(800)
            } catch (e: Exception) {
            }
            val endIcon = driver.findElements(AppiumBy.id("$APP_PACKAGE:id/text_input_end_icon")).firstOrNull()
            if (endIcon != null) {
                endIcon.click()
            }
            Thread.sleep(2000)
            val source = runCatching { driver.pageSource }.getOrDefault("")
            val inSource = source.contains(name)
            if (inSource) {
                var item = driver.findElements(
                    AppiumBy.xpath("//android.widget.TextView[@text='$name']")
                ).firstOrNull()
                if (item == null) {
                    item = driver.findElements(
                        AppiumBy.androidUIAutomator("new UiSelector().text(\"$name\")")
                    ).firstOrNull()
                }
                if (item != null) {
                    item.click()
                    Thread.sleep(2000)
                    val after = runCatching {
                        driver.findElement(AppiumBy.id("$APP_PACKAGE:id/news_category_auto_complete")).text
                    }.getOrDefault("").trim()
                    if (after == name) return true
                }
            }
            if (inSource) {
                driver.navigate().back()
                Thread.sleep(1500)
            }
        }
        return false
    }

    private fun setFilterCategory(name: String): Boolean {
        for (attempt in 1..3) {
            val field = driver.findElements(
                AppiumBy.id("$APP_PACKAGE:id/filter_news_category_auto_complete")
            ).firstOrNull() ?: return false
            if (field.text.trim() == name) return true
            try {
                field.click()
                Thread.sleep(500)
                field.clear()
                field.sendKeys(name)
                Thread.sleep(1000)
                try {
                    driver.hideKeyboard()
                } catch (e: Exception) {
                }
                Thread.sleep(500)
                val committed = driver.findElements(
                    AppiumBy.id("$APP_PACKAGE:id/filter_news_category_auto_complete")
                ).firstOrNull()?.text?.trim() ?: ""
                if (committed == name) return true
            } catch (t: Throwable) {
            }
        }
        return false
    }

    private fun openControlPanel() {
        Allure.step("Открыть панель управления через меню") {
            mainPage.tapMainMenu()
            mainPage.tapMenuNews()
            assertTrue("Панель управления не открылась", newsScreen.isScreenOpened())
        }
    }

    private fun isCreatingNewsScreenVisible(): Boolean {
        val saveById = driver.findElements(AppiumBy.id("$APP_PACKAGE:id/news_save_button"))
        val saveByText = driver.findElements(
            AppiumBy.xpath("//android.widget.Button[contains(@text,'SAVE') or contains(@text,'Сохранить')]")
        )
        val cancelById = driver.findElements(AppiumBy.id("$APP_PACKAGE:id/news_cancel_button"))
        return saveById.isNotEmpty() || saveByText.isNotEmpty() || cancelById.isNotEmpty()
    }

    private fun tapSaveButton(): Boolean {
        try {
            driver.hideKeyboard()
        } catch (e: Exception) {
        }
        Thread.sleep(1000)
        var saveButton = driver.findElements(AppiumBy.id("$APP_PACKAGE:id/news_save_button"))
            .firstOrNull()
        if (saveButton == null) {
            saveButton = driver.findElements(
                AppiumBy.xpath("//android.widget.Button[contains(@text,'SAVE') or contains(@text,'Сохранить')]")
            ).firstOrNull()
        }
        if (saveButton == null) {
            try {
                driver.findElement(
                    AppiumBy.androidUIAutomator(
                        "new UiScrollable(new UiSelector().scrollable(true)).scrollForward()"
                    )
                )
                Thread.sleep(1000)
            } catch (e: Exception) {
            }
            saveButton = driver.findElements(AppiumBy.id("$APP_PACKAGE:id/news_save_button"))
                .firstOrNull()
                ?: driver.findElements(
                    AppiumBy.xpath("//android.widget.Button[contains(@text,'SAVE') or contains(@text,'Сохранить')]")
                ).firstOrNull()
        }
        if (saveButton == null) return false
        saveButton.click()
        Thread.sleep(2500)
        return true
    }

    private fun tapAddNewsButton(): Boolean {
        val deadline = System.currentTimeMillis() + 20000
        while (System.currentTimeMillis() < deadline) {
            val plus = driver.findElements(AppiumBy.id("$APP_PACKAGE:id/add_news_image_view"))
            if (plus.isNotEmpty()) {
                plus.first().click()
                Thread.sleep(2500)
                return isCreatingNewsScreenVisible()
            }
            val edit = driver.findElements(AppiumBy.id("$APP_PACKAGE:id/news_edit_button"))
            if (edit.isNotEmpty()) {
                edit.first().click()
                Thread.sleep(2500)
                continue
            }
            Thread.sleep(500)
        }
        return false
    }

    private fun tapEditFirstNews(): Boolean {
        val deadline = System.currentTimeMillis() + 10000
        while (System.currentTimeMillis() < deadline) {
            val list = driver.findElements(AppiumBy.id("$APP_PACKAGE:id/news_item_edit_image_view"))
            if (list.isNotEmpty()) {
                list.first().click()
                Thread.sleep(2500)
                return isCreatingNewsScreenVisible()
            }
            Thread.sleep(500)
        }
        return false
    }

    private fun ensureControlPanelList() {
        Allure.step("Открыть Control panel со списком новостей") {
            val deadline = System.currentTimeMillis() + 15000
            var hasPencils = false
            while (System.currentTimeMillis() < deadline) {
                if (driver.findElements(AppiumBy.id("$APP_PACKAGE:id/news_item_edit_image_view")).isNotEmpty()) {
                    hasPencils = true
                    break
                }
                driver.findElements(AppiumBy.id("$APP_PACKAGE:id/news_edit_button")).firstOrNull()?.let {
                    it.click()
                    Thread.sleep(2000)
                }
                Thread.sleep(500)
            }
            assertTrue("Не открылся список Control panel с иконками", hasPencils)
        }
    }

    private fun createNewsViaUI(title: String, description: String, category: String = "Объявление"): Boolean {
        val plusVisible = driver.findElements(AppiumBy.id("$APP_PACKAGE:id/add_news_image_view")).isNotEmpty()
        if (!plusVisible) {
            ensureMainScreen()
            openControlPanel()
        }
        if (!tapAddNewsButton()) {
            return false
        }
        return try {
            if (!selectCategory(category)) return false
            driver.findElement(AppiumBy.id("$APP_PACKAGE:id/news_title_edit_text")).sendKeys(title)
            driver.findElement(AppiumBy.id("$APP_PACKAGE:id/news_description_edit_text"))
                .sendKeys(description)
            driver.findElement(AppiumBy.id("$APP_PACKAGE:id/news_publish_date_edit_text")).click()
            confirmPickerDialog()
            driver.findElement(AppiumBy.id("$APP_PACKAGE:id/news_publish_time_edit_text")).click()
            confirmPickerDialog()
            var closed = false
            val saveDeadline = System.currentTimeMillis() + 20000
            while (!closed && System.currentTimeMillis() < saveDeadline) {
                tapSaveButton()
                Thread.sleep(1500)
                if (!isCreatingNewsScreenVisible()) {
                    closed = true
                    break
                }
            }
            closed
        } catch (t: Throwable) {
            false
        }
    }

    private fun waitForTitleInList(title: String): Boolean {
        val deadline = System.currentTimeMillis() + 10000
        while (System.currentTimeMillis() < deadline) {
            val titles = driver.findElements(AppiumBy.id("$APP_PACKAGE:id/news_item_title_text_view"))
                .map { it.text }
            if (title in titles) return true
            Thread.sleep(1000)
        }
        return false
    }

    private fun waitForTitleGone(title: String): Boolean {
        val deadline = System.currentTimeMillis() + 10000
        while (System.currentTimeMillis() < deadline) {
            val titles = driver.findElements(AppiumBy.id("$APP_PACKAGE:id/news_item_title_text_view"))
                .map { it.text }
            if (title !in titles) return true
            Thread.sleep(1000)
        }
        return false
    }

    private fun ensureNewsAvailable() {
        Allure.step("Обеспечить наличие новостей") {
            var cards = isNewsCardsVisible()
            val deadline = System.currentTimeMillis() + 10000
            while (!cards && System.currentTimeMillis() < deadline) {
                Thread.sleep(1000)
                cards = isNewsCardsVisible()
            }
            if (!cards) {
                newsScreen.tapRefresh()
                Thread.sleep(2000)
                cards = isNewsCardsVisible()
            }
            if (!cards) {
                createNewsViaUI(
                    "Тестовая новость ${System.currentTimeMillis()}",
                    "Описание тестовой новости"
                )
                driver.navigate().back()
                Thread.sleep(1500)
                ensureMainScreen()
                mainPage.tapAllNews()
                newsScreen.isScreenOpened()
                cards = isNewsCardsVisible()
            }
            assertTrue("Карточек новостей нет даже после попытки создания", cards)
        }
    }

    private fun openFilter() {
        Allure.step("Открыть экран фильтра") {
            newsScreen.tapFilter()
            assertTrue("Экран фильтра не открылся", filterScreen.isOpened())
        }
    }

    private fun applyFilter() {
        Allure.step("Применить фильтр") {
            filterScreen.hideKeyboard()
            Thread.sleep(1000)
            filterScreen.tapApply()
            Thread.sleep(3000)
            assertTrue("После FILTER экран фильтра не закрылся", !filterScreen.isOpened())
        }
    }

    private fun applyDateFilter(startText: String?, endText: String?) {
        Allure.step("Задать даты фильтра") {
            if (startText != null) {
                assertTrue("Не удалось задать начальную дату", filterScreen.enterDate(filterScreen.dateStart, startText))
            }
            if (endText != null) {
                assertTrue("Не удалось задать конечную дату", filterScreen.enterDate(filterScreen.dateEnd, endText))
            }
        }
        applyFilter()
    }

    private fun openAllNewsFromMain() {
        Allure.step("Открыть ALL NEWS") {
            ensureMainScreen()
            mainPage.tapAllNews()
            newsScreen.isScreenOpened()
        }
    }

    @Test
    @Story("Авторизация")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Успешная авторизация: открыт главный экран")
    @Description("Пользователь входит с валидными данными и попадает на главный экран")
    fun testSuccessfulLogin() {
        ensureMainScreen()
    }

    @Test
    @Story("Лента новостей")
    @DisplayName("Отображение списка новостей")
    @Description("На экране ALL NEWS отображаются карточки новостей")
    fun testNewsListDisplayed() {
        openAllNewsFromMain()
        Allure.step("Проверка списка новостей") {
            assertTrue("На экране ALL NEWS нет ни одной карточки новости", isNewsCardsVisible())
        }
    }

    @Test
    @Story("Лента новостей")
    @DisplayName("Раскрытие и сворачивание карточки новости")
    @Description("Клик по карточке раскрывает описание, повторный клик сворачивает")
    fun testExpandNewsCard() {
        ensureMainScreen()
        Allure.step("Проверка раскрытия карточки") {
            val wasExpanded = mainPage.isPresent(mainPage.firstCardDescription)
            mainPage.tapFirstCardTitle()
            val isExpandedNow = mainPage.isPresent(mainPage.firstCardDescription)
            assertTrue("Клик по карточке не изменил состояние раскрытия", isExpandedNow != wasExpanded)
            mainPage.tapFirstCardTitle()
            assertTrue(
                "Повторный клик не вернул карточку в исходное состояние",
                mainPage.isPresent(mainPage.firstCardDescription) == wasExpanded
            )
        }
    }

    @Test
    @Story("Лента новостей")
    @DisplayName("Переход на экран ALL NEWS")
    @Description("Кнопка ALL NEWS открывает полный список новостей")
    fun testOpenAllNews() {
        openAllNewsFromMain()
        Allure.step("Проверка экрана списка новостей") {
            var visible = newsScreen.isListVisible()
            val deadline = System.currentTimeMillis() + 15000
            while (!visible && System.currentTimeMillis() < deadline) {
                Thread.sleep(1000)
                visible = newsScreen.isListVisible()
            }
            assertTrue("На экране ALL NEWS нет ни карточек, ни заглушки", newsScreen.isListVisible())
        }
    }

    @Test
    @Story("Тематические цитаты")
    @DisplayName("Переход на экран Our Mission")
    @Description("Экран Our Mission открывается и содержит цитаты")
    fun testOpenOurMission() {
        ensureMainScreen()
        Allure.step("Открыть Our Mission") {
            mainPage.tapOurMission()
        }
        Allure.step("Проверка цитат на экране") {
            assertTrue("На экране Our Mission нет цитат", aboutScreen.isQuotesVisible())
        }
    }

    @Test
    @Story("Навигация")
    @DisplayName("Навигация через главное меню")
    @Description("Через главное меню открывается раздел News со списком новостей")
    fun testOpenMainMenuNavigation() {
        ensureMainScreen()
        openControlPanel()
        Allure.step("Проверка раздела News") {
            assertTrue("После перехода через меню раздел News не открылся", isNewsCardsVisible())
        }
    }

    @Test
    @Story("Авторизация")
    @DisplayName("Выход из приложения")
    @Description("После Log out открывается экран авторизации")
    fun testLogout() {
        ensureMainScreen()
        Allure.step("Нажать Log out в меню профиля") {
            mainPage.tapAuthorization()
            mainPage.tapLogout()
        }
        Allure.step("Проверка экрана авторизации") {
            assertTrue("После выхода не открылся экран авторизации", loginPage.isDisplayed(15))
        }
    }

    @Test
    @Story("Работа с новостями")
    @DisplayName("Переключение сортировки новостей")
    @Description("Кнопка сортировки переключает состояние и возвращает его повторным нажатием")
    fun testSortNewsButton() {
        openAllNewsFromMain()
        Allure.step("Переключить сортировку и вернуть обратно") {
            val stateBefore = newsScreen.sortChecked()
            var stateAfter = stateBefore
            val deadline = System.currentTimeMillis() + 8000
            while (System.currentTimeMillis() < deadline) {
                newsScreen.tapSort()
                Thread.sleep(1500)
                stateAfter = newsScreen.sortChecked()
                if (stateAfter != stateBefore) break
            }
            assertTrue(
                "Сортировка не переключилась (before=$stateBefore, after=$stateAfter)",
                stateAfter != stateBefore
            )
            var backState = stateAfter
            val deadline2 = System.currentTimeMillis() + 8000
            while (System.currentTimeMillis() < deadline2) {
                newsScreen.tapSort()
                Thread.sleep(1500)
                backState = newsScreen.sortChecked()
                if (backState == stateBefore) break
            }
            assertTrue(
                "Повторное нажатие не вернуло сортировку (before=$stateBefore, back=$backState)",
                backState == stateBefore
            )
        }
    }

    @Test
    @Story("Работа с новостями")
    @DisplayName("Открытие экрана фильтра")
    @Description("Кнопка фильтра открывает экран Filter news с категорией и полями дат")
    fun testFilterNewsButtonOpens() {
        openAllNewsFromMain()
        openFilter()
        Allure.step("Проверка элементов фильтра") {
            assertTrue("Нет поля категории", filterScreen.isPresent(filterScreen.categoryField))
            assertTrue("Нет поля начальной даты", filterScreen.isPresent(filterScreen.dateStart))
            assertTrue("Нет поля конечной даты", filterScreen.isPresent(filterScreen.dateEnd))
        }
        Allure.step("Закрыть фильтр") {
            filterScreen.close()
            newsScreen.isScreenOpened(10)
        }
    }

    @Test
    @Story("Работа с новостями")
    @DisplayName("Применение фильтра по датам")
    @Description("После фильтра по диапазону дат остаются только карточки внутри диапазона")
    fun testFilterNewsApply() {
        val start = LocalDate.now().minusDays(java.util.Random().nextInt(5).toLong())
        val end = LocalDate.now().plusYears(4).plusDays(java.util.Random().nextInt(365).toLong())
        val startText = start.format(FORMATTER)
        val endText = end.format(FORMATTER)
        openAllNewsFromMain()
        ensureNewsAvailable()
        Allure.step("Применить фильтр $startText — $endText") {
            openFilter()
            applyDateFilter(startText, endText)
        }
        Allure.step("Проверка результата фильтрации") {
            val datesAfter = getVisibleNewsDates()
            assertTrue(
                "После фильтра не осталось ни одной карточки (диапазон $startText — $endText)",
                datesAfter.isNotEmpty()
            )
            val outOfRange = datesAfter.count { it.isBefore(start) || it.isAfter(end) }
            assertTrue(
                "После фильтра остались карточки вне диапазона $startText — $endText: $datesAfter",
                outOfRange == 0
            )
        }
    }

    @Test
    @Story("Работа с новостями")
    @DisplayName("Применение фильтра по категории")
    @Description("После фильтра по категории новость этой категории остаётся, чужие категории исчезают")
    fun testFilterNewsByCategory() {
        val announcementTitle = "Фильтр Объявление ${System.currentTimeMillis()}"
        Allure.step("Создать новость категории 'Объявление'") {
            ensureMainScreen()
            assertTrue(
                "Не удалось создать новость 'Объявление'",
                createNewsViaUI(announcementTitle, "Для фильтра по категории", "Объявление")
            )
        }
        Allure.step("Применить фильтр по категории 'Объявление'") {
            if (!mainPage.isMainScreen()) {
                driver.navigate().back()
                Thread.sleep(1500)
            }
            ensureMainScreen()
            openAllNewsReliably()
            openFilter()
            assertTrue("Не удалось задать категорию фильтра", setFilterCategory("Объявление"))
            applyFilter()
        }
        Allure.step("Проверка результата фильтрации") {
            val deadline = System.currentTimeMillis() + 10000
            var sawAnnouncement = false
            while (System.currentTimeMillis() < deadline) {
                val titles = driver.findElements(AppiumBy.id("$APP_PACKAGE:id/news_item_title_text_view"))
                    .map { it.text }
                if (announcementTitle in titles) {
                    sawAnnouncement = true
                    break
                }
                Thread.sleep(1000)
            }
            assertTrue("Новость '$announcementTitle' не найдена после фильтра", sawAnnouncement)
            val titlesNow = driver.findElements(AppiumBy.id("$APP_PACKAGE:id/news_item_title_text_view"))
                .map { it.text }
            assertTrue("Новость другой категории не отфильтровалась, видны: $titlesNow",
                titlesNow.none { it.contains("Праздник") })
        }
    }

    @Test
    @Story("Работа с новостями")
    @DisplayName("Открытие экрана создания и редактирования новости")
    @Description("Кнопка '+' и карандаш на карточке открывают экран с формой и кнопкой SAVE")
    fun testNewsEditButtonOpens() {
        ensureMainScreen()
        openControlPanel()
        Allure.step("Клик по '+': открылся экран с SAVE") {
            assertTrue("После '+' экран с SAVE не открылся", tapAddNewsButton())
        }
        Allure.step("Вернуться назад") {
            driver.navigate().back()
            Thread.sleep(1500)
        }
        Allure.step("Клик по карандашу: открылся экран с SAVE") {
            assertTrue("После карандаша экран с SAVE не открылся", tapEditFirstNews())
        }
        Allure.step("Вернуться назад") {
            driver.navigate().back()
            Thread.sleep(1500)
        }
    }

    @Test
    @Story("Работа с новостями")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Создание новости")
    @Description("Клик по '+', заполнение формы и сохранение новой новости")
    fun testCreateNews() {
        val newsTitle = "Тестовая новость ${System.currentTimeMillis()}"
        ensureMainScreen()
        openControlPanel()
        Allure.step("Открыть форму через '+'") {
            assertTrue("После '+' экран с SAVE не открылся", tapAddNewsButton())
        }
        Allure.step("Выбрать категорию") {
            assertTrue("Не удалось выбрать категорию", selectCategory("Объявление"))
        }
        Allure.step("Заполнить название и описание") {
            driver.findElement(AppiumBy.id("$APP_PACKAGE:id/news_title_edit_text")).sendKeys(newsTitle)
            driver.findElement(AppiumBy.id("$APP_PACKAGE:id/news_description_edit_text"))
                .sendKeys("Описание тестовой новости")
        }
        Allure.step("Установить дату и время") {
            driver.findElement(AppiumBy.id("$APP_PACKAGE:id/news_publish_date_edit_text")).click()
            confirmPickerDialog()
            driver.findElement(AppiumBy.id("$APP_PACKAGE:id/news_publish_time_edit_text")).click()
            confirmPickerDialog()
        }
        Allure.step("Сохранить новость") {
            try {
                driver.hideKeyboard()
            } catch (e: Exception) {
            }
            Thread.sleep(1000)
            val categoryBeforeSave = runCatching {
                driver.findElement(AppiumBy.id("$APP_PACKAGE:id/news_category_auto_complete")).text
            }.getOrDefault("")
            if (categoryBeforeSave.trim() != "Объявление") {
                assertTrue("Не удалось выбрать категорию", selectCategory("Объявление"))
            }
            var saved = false
            for (attempt in 1..3) {
                val ok = tapSaveButton()
                if (!ok) break
                if (!isCreatingNewsScreenVisible()) {
                    saved = true
                    break
                }
            }
            assertTrue("SAVE не принял форму после 3 попыток", saved && !isCreatingNewsScreenVisible())
        }
        Allure.step("Проверка: новость появилась в списке") {
            assertTrue("Созданная новость '$newsTitle' не найдена в списке", waitForTitleInList(newsTitle))
        }
    }

    @Test
    @Story("Работа с новостями")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Редактирование новости")
    @Description("Открываем новость карандашом, меняем заголовок, сохраняем и проверяем в списке")
    fun testEditNews() {
        val newTitle = "Отредактировано ${System.currentTimeMillis()}"
        ensureMainScreen()
        openControlPanel()
        ensureControlPanelList()
        Allure.step("Открыть первую новость на редактирование") {
            assertTrue("Не открылся экран редактирования", tapEditFirstNews())
        }
        Allure.step("Изменить заголовок") {
            val titleField = driver.findElement(AppiumBy.id("$APP_PACKAGE:id/news_title_edit_text"))
            val original = titleField.text
            assertTrue("Исходный заголовок пуст — возможно, открыта не та форма", original.isNotBlank())
            titleField.click()
            titleField.clear()
            titleField.sendKeys(newTitle)
            Thread.sleep(500)
        }
        Allure.step("Сохранить изменения") {
            var closed = false
            for (attempt in 1..3) {
                val ok = tapSaveButton()
                if (!ok) continue
                if (!isCreatingNewsScreenVisible()) {
                    closed = true
                    break
                }
            }
            assertTrue("SAVE не принял изменения", closed)
        }
        Allure.step("Проверка: новый заголовок в списке") {
            assertTrue("Изменённый заголовок '$newTitle' не найден в списке", waitForTitleInList(newTitle))
        }
    }

    @Test
    @Story("Работа с новостями")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Удаление новости")
    @Description("Создаём тестовую новость и удаляем её через иконку корзины на карточке")
    fun testDeleteNews() {
        val newsTitle = "На удаление ${System.currentTimeMillis()}"
        ensureMainScreen()
        openControlPanel()
        Allure.step("Создать тестовую новость для удаления") {
            assertTrue("Не открыли форму через '+'", tapAddNewsButton())
            assertTrue("Категория не выбралась", selectCategory("Объявление"))
            driver.findElement(AppiumBy.id("$APP_PACKAGE:id/news_title_edit_text")).sendKeys(newsTitle)
            driver.findElement(AppiumBy.id("$APP_PACKAGE:id/news_description_edit_text"))
                .sendKeys("Новость для проверки удаления")
            driver.findElement(AppiumBy.id("$APP_PACKAGE:id/news_publish_date_edit_text")).click()
            confirmPickerDialog()
            driver.findElement(AppiumBy.id("$APP_PACKAGE:id/news_publish_time_edit_text")).click()
            confirmPickerDialog()
            val saved = tapSaveButton()
            assertTrue("SAVE не принял форму", saved && !isCreatingNewsScreenVisible())
        }
        ensureControlPanelList()
        Allure.step("Удалить новость через иконку на карточке") {
            var deleted = false
            val deadline = System.currentTimeMillis() + 10000
            while (System.currentTimeMillis() < deadline && !deleted) {
                val cards = driver.findElements(AppiumBy.id("$APP_PACKAGE:id/news_item_material_card_view"))
                for (card in cards) {
                    val hasTitle = card.findElements(AppiumBy.id("$APP_PACKAGE:id/news_item_title_text_view"))
                        .any { it.text == newsTitle }
                    if (hasTitle) {
                        val del = card.findElements(AppiumBy.id("$APP_PACKAGE:id/news_item_delete_image_view"))
                        if (del.isNotEmpty()) {
                            del.first().click()
                            Thread.sleep(1500)
                            val confirm = driver.findElements(AppiumBy.id("android:id/button1")).firstOrNull()
                                ?: driver.findElements(
                                    AppiumBy.xpath(
                                        "//android.widget.Button[contains(@text,'DELETE') or contains(@text,'Удалить') or contains(@text,'OK')]"
                                    )
                                ).firstOrNull()
                            assertTrue("Диалог подтверждения не открылся", confirm != null)
                            confirm!!.click()
                            Thread.sleep(2500)
                            deleted = true
                            break
                        }
                    }
                }
                if (!deleted) Thread.sleep(1000)
            }
            assertTrue("Новость '$newsTitle' не удалось удалить", deleted)
        }
        Allure.step("Проверка: новость исчезла из списка") {
            assertTrue("Удалённая новость '$newsTitle' осталась в списке", waitForTitleGone(newsTitle))
        }
    }

    @Test
    @Story("Негативные сценарии")
    @DisplayName("Сохранение пустой формы новости")
    @Description("SAVE на пустой форме не закрывает её: обязательные поля не заполнены")
    fun testSaveEmptyForm() {
        ensureMainScreen()
        openControlPanel()
        Allure.step("Открыть форму через '+'") {
            assertTrue("После '+' экран с SAVE не открылся", tapAddNewsButton())
        }
        Allure.step("Нажать SAVE без заполнения полей") {
            tapSaveButton()
            assertTrue("Пустая форма сохранилась — валидация не сработала", isCreatingNewsScreenVisible())
        }
        Allure.step("Выйти из формы без сохранения") {
            driver.navigate().back()
            Thread.sleep(1500)
        }
    }

    @Test
    @Story("Негативные сценарии")
    @DisplayName("Редактирование с пустым заголовком")
    @Description("SAVE с очищенным заголовком не закрывает форму: поле обязательное")
    fun testEditEmptyTitle() {
        ensureMainScreen()
        openControlPanel()
        ensureControlPanelList()
        Allure.step("Открыть первую новость на редактирование") {
            assertTrue("Не открылся экран редактирования", tapEditFirstNews())
        }
        Allure.step("Очистить заголовок и нажать SAVE") {
            val titleField = driver.findElement(AppiumBy.id("$APP_PACKAGE:id/news_title_edit_text"))
            titleField.click()
            titleField.clear()
            Thread.sleep(500)
            tapSaveButton()
            assertTrue("Форма с пустым заголовком сохранилась", isCreatingNewsScreenVisible())
        }
        Allure.step("Выйти из формы без сохранения") {
            driver.navigate().back()
            Thread.sleep(1500)
        }
    }

    @Test
    @Story("Негативные сценарии")
    @DisplayName("Отмена создания новости")
    @Description("После CANCEL новость не появляется в списке")
    fun testCancelCreation() {
        val newsTitle = "Отменённая новость ${System.currentTimeMillis()}"
        ensureMainScreen()
        openControlPanel()
        Allure.step("Заполнить форму и нажать CANCEL") {
            assertTrue("Не открыли форму через '+'", tapAddNewsButton())
            driver.findElement(AppiumBy.id("$APP_PACKAGE:id/news_title_edit_text")).sendKeys(newsTitle)
            driver.findElement(AppiumBy.id("$APP_PACKAGE:id/news_description_edit_text"))
                .sendKeys("Новость, которая не должна сохраниться")
            val cancel = driver.findElements(AppiumBy.id("$APP_PACKAGE:id/news_cancel_button")).firstOrNull()
                ?: driver.findElements(
                    AppiumBy.xpath("//android.widget.Button[contains(@text,'CANCEL') or contains(@text,'Отмена')]")
                ).firstOrNull()
            assertTrue("Кнопка CANCEL не найдена", cancel != null)
            cancel!!.click()
            Thread.sleep(2000)
            assertTrue("После CANCEL форма осталась открытой", !isCreatingNewsScreenVisible())
        }
        Allure.step("Проверка: новость не появилась в списке") {
            val titles = driver.findElements(AppiumBy.id("$APP_PACKAGE:id/news_item_title_text_view"))
                .map { it.text }
            assertTrue(
                "Отменённая новость '$newsTitle' попала в список: $titles",
                newsTitle !in titles
            )
        }
    }

    @Test
    @Story("Работа с новостями")
    @DisplayName("Фильтр с только начальной датой (известный дефект)")
    @Description("Дефект: при заполненной только начальной дате фильтр возвращает пустой список, хотя подходящие новости есть. Задокументирован в баг-репорте №3")
    fun testFilterStartDateOnly() {
        val startText = LocalDate.now().minusDays(1).format(FORMATTER)
        openAllNewsFromMain()
        Allure.step("Проверить наличие новостей до фильтра") {
            assertTrue("Перед фильтром нет карточек — проверка дефекта невозможна",
                getVisibleNewsDates().isNotEmpty())
        }
        Allure.step("Применить фильтр с только начальной датой $startText") {
            openFilter()
            applyDateFilter(startText, null)
        }
        Allure.step("Проверка документированного поведения") {
            val datesAfter = getVisibleNewsDates()
            assertTrue(
                "После фильтра появились карточки: $datesAfter — начальная дата применена корректно, дефект мог быть исправлен; обнови тест и баг-репорт №3",
                datesAfter.isEmpty()
            )
        }
    }

    @Test
    @Story("Работа с новостями")
    @DisplayName("Фильтр с только конечной датой")
    @Description("После фильтра с заполненной только конечной датой остаются карточки с датой не позже неё")
    fun testFilterEndDateOnly() {
        val end = LocalDate.now().minusDays(1)
        val endText = end.format(FORMATTER)
        openAllNewsFromMain()
        Allure.step("Применить фильтр с только конечной датой $endText") {
            openFilter()
            applyDateFilter(null, endText)
        }
        Allure.step("Проверка результата фильтрации") {
            val datesAfter = getVisibleNewsDates()
            val tooLate = datesAfter.count { it.isAfter(end) }
            assertTrue("После фильтра остались карточки позже $endText: $datesAfter", tooLate == 0)
        }
    }
}
