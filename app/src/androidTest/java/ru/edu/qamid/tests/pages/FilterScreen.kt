package ru.edu.qamid.tests.pages

import android.util.Log
import io.appium.java_client.AppiumBy
import io.appium.java_client.android.AndroidDriver
import io.appium.java_client.android.nativekey.AndroidKey
import io.appium.java_client.android.nativekey.KeyEvent
import org.openqa.selenium.By

class FilterScreen(private val driver: AndroidDriver) {

    val title: By = AppiumBy.id("ru.edu.qamid:id/filter_news_title_text_view")
    val categoryField: By = AppiumBy.id("ru.edu.qamid:id/filter_news_category_auto_complete")
    val dateStart: By = AppiumBy.id("ru.edu.qamid:id/filter_news_date_start_edit_text")
    val dateEnd: By = AppiumBy.id("ru.edu.qamid:id/filter_news_date_end_edit_text")
    val applyButton: By = AppiumBy.id("ru.edu.qamid:id/filter_news_apply_button")
    val cancelButton: By = AppiumBy.id("ru.edu.qamid:id/filter_news_cancel_button")

    fun isOpened(timeoutSeconds: Long = 10): Boolean {
        val deadline = System.currentTimeMillis() + timeoutSeconds * 1000
        while (System.currentTimeMillis() < deadline) {
            try {
                driver.findElement(title)
                Thread.sleep(500)
                return true
            } catch (e: Exception) {
                Thread.sleep(500)
            }
        }
        return false
    }

    fun isPresent(locator: By): Boolean =
        driver.findElements(locator).isNotEmpty()

    fun dismissPossibleDialog() {
        driver.pressKey(KeyEvent(AndroidKey.BACK))
        Thread.sleep(1000)
    }

    fun enterDate(locator: By, dateValue: String): Boolean {
        for (attempt in 1..3) {
            try {
                val fieldBefore = driver.findElements(locator).firstOrNull() ?: continue
                fieldBefore.click()
                Thread.sleep(1500)

                val okButton = driver.findElements(AppiumBy.id("android:id/button1")).firstOrNull()
                Log.i("TEST_DIAG", "Попытка $attempt: диалог = ${okButton != null}")

                if (okButton != null) {
                    okButton.click()
                    Thread.sleep(1500)
                    val current = driver.findElements(locator).firstOrNull()?.text ?: ""
                    Log.i("TEST_DIAG", "Попытка $attempt: поле после диалога = '$current'")
                    if (current.isNotBlank()) {
                        return true
                    }
                    continue
                }

                val fieldAfter = driver.findElements(locator).firstOrNull() ?: continue
                fieldAfter.clear()
                fieldAfter.sendKeys(dateValue)
                Thread.sleep(500)
                val current = driver.findElements(locator).firstOrNull()?.text ?: ""
                Log.i("TEST_DIAG", "Попытка $attempt: текст после ввода = '$current'")
                if (current.isNotBlank()) {
                    return true
                }
                dismissPossibleDialog()
            } catch (e: Exception) {
                Log.i(
                    "TEST_DIAG",
                    "Попытка $attempt: исключение ${e.javaClass.simpleName}: ${e.message?.take(200)}"
                )
                dismissPossibleDialog()
            }
        }
        return false
    }

    fun hideKeyboard() {
        try {
            driver.hideKeyboard()
        } catch (e: Exception) {
        }
        Thread.sleep(500)
    }

    fun tapApply() {
        driver.findElement(applyButton).click()
        Thread.sleep(2000)
    }

    fun close() {
        hideKeyboard()
        if (isPresent(cancelButton)) {
            driver.findElement(cancelButton).click()
            Thread.sleep(1500)
        } else {
            driver.navigate().back()
            Thread.sleep(1500)
        }
    }
}
