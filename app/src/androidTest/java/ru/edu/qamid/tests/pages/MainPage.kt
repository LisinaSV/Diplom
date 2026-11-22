package ru.edu.qamid.tests.pages

import io.appium.java_client.AppiumBy
import io.appium.java_client.android.AndroidDriver
import org.openqa.selenium.By

class MainPage(private val driver: AndroidDriver) {

    private val newsTitle: By = AppiumBy.androidUIAutomator("new UiSelector().text(\"News\")")
    private val allNewsButton: By = AppiumBy.id("ru.edu.qamid:id/all_news_text_view")
    private val mainMenuButton: By = AppiumBy.id("ru.edu.qamid:id/main_menu_image_button")
    private val authButton: By = AppiumBy.id("ru.edu.qamid:id/authorization_image_button")

    val newsListScreen: By = AppiumBy.id("ru.edu.qamid:id/news_list_recycler_view")

    val firstNewsCard: By = AppiumBy.androidUIAutomator(
        "new UiSelector().resourceId(\"ru.edu.qamid:id/news_item_material_card_view\").instance(0)"
    )
    val firstCardDescription: By = AppiumBy.androidUIAutomator(
        "new UiSelector().resourceId(\"ru.edu.qamid:id/news_item_description_text_view\").instance(0)"
    )
    val firstCardTitle: By = AppiumBy.androidUIAutomator(
        "new UiSelector().resourceId(\"ru.edu.qamid:id/news_item_title_text_view\").instance(0)"
    )

    fun isMainScreen(timeoutSeconds: Long = 10): Boolean {
        val deadline = System.currentTimeMillis() + timeoutSeconds * 1000
        while (System.currentTimeMillis() < deadline) {
            try {
                driver.findElement(newsTitle)
                return true
            } catch (e: Exception) {
                Thread.sleep(500)
            }
        }
        return false
    }

    fun isPresent(locator: By): Boolean =
        driver.findElements(locator).isNotEmpty()

    fun tapAllNews() {
        driver.findElement(allNewsButton).click()
        Thread.sleep(2000)
    }

    fun tapOurMission() {
        driver.findElement(
            AppiumBy.androidUIAutomator("new UiSelector().description(\"Our Mission\")")
        ).click()
        Thread.sleep(2000)
    }

    fun tapMainMenu() {
        driver.findElement(mainMenuButton).click()
        Thread.sleep(1500)
    }

    fun tapMenuNews() {
        driver.findElement(newsTitle).click()
        Thread.sleep(2000)
    }

    fun tapAuthorization() {
        driver.findElement(authButton).click()
        Thread.sleep(1500)
    }

    fun tapLogout() {
        driver.findElement(
            AppiumBy.androidUIAutomator("new UiSelector().text(\"Log out\")")
        ).click()
        Thread.sleep(2000)
    }

    fun tapFirstCardTitle() {
        driver.findElement(firstCardTitle).click()
        Thread.sleep(1500)
    }
}
