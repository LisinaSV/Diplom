package ru.edu.qamid.tests.pages

import io.appium.java_client.AppiumBy
import io.appium.java_client.android.AndroidDriver
import org.openqa.selenium.By

class NewsScreen(private val driver: AndroidDriver) {

    private companion object {
        const val PKG = "ru.edu.qamid"
    }

    val sortButton: By = AppiumBy.id("$PKG:id/news_sort_button")
    val filterButton: By = AppiumBy.id("$PKG:id/news_filter_button")
    val addNewsButton: By = AppiumBy.id("$PKG:id/add_news_image_view")
    val editCardButton: By = AppiumBy.id("$PKG:id/news_item_edit_image_view")
    val recyclerView: By = AppiumBy.id("$PKG:id/news_list_recycler_view")
    val newsItemTitle: By = AppiumBy.id("$PKG:id/news_item_title_text_view")


    val emptyStateText: By =
        AppiumBy.xpath("//android.widget.TextView[contains(@text,'nothing here yet')]")
    val refreshButton: By =
        AppiumBy.xpath("//android.widget.Button[@text='REFRESH']")

    fun isScreenOpened(timeoutSeconds: Long = 10): Boolean {
        val deadline = System.currentTimeMillis() + timeoutSeconds * 1000
        while (System.currentTimeMillis() < deadline) {
            try {

                if (driver.findElements(recyclerView).isNotEmpty()) return true
                if (driver.findElements(emptyStateText).isNotEmpty()) return true
            } catch (e: Exception) {
            }
            Thread.sleep(500)
        }
        return false
    }

    fun isEmptyStateVisible(): Boolean = try {
        driver.findElements(emptyStateText).isNotEmpty()
    } catch (e: Exception) {
        false
    }

    fun isListVisible(): Boolean = try {
        driver.findElements(recyclerView).isNotEmpty() ||
                driver.findElements(newsItemTitle).isNotEmpty() ||
                driver.findElements(emptyStateText).isNotEmpty()
    } catch (e: Exception) {
        false
    }

    fun tapRefresh(): Boolean = try {
        val button = driver.findElements(refreshButton).firstOrNull()
        if (button != null) {
            button.click()
            Thread.sleep(2000)
            true
        } else {
            false
        }
    } catch (e: Exception) {
        false
    }

    fun tapSort() {
        driver.findElement(sortButton).click()
        Thread.sleep(1500)
    }

    fun sortChecked(): Boolean = try {
        driver.findElement(sortButton).getAttribute("checked") == "true"
    } catch (e: Exception) {
        false
    }

    fun tapFilter() {
        driver.findElement(filterButton).click()
        Thread.sleep(1500)
    }

    fun tapEdit(): Boolean {
        val deadline = System.currentTimeMillis() + 10000
        while (System.currentTimeMillis() < deadline) {
            val button = try {
                driver.findElements(addNewsButton).firstOrNull()
            } catch (e: Exception) {
                null
            }
            if (button != null) {
                button.click()
                Thread.sleep(2000)
                return true
            }
            Thread.sleep(500)
        }
        return false
    }
}
