package ru.edu.qamid.tests.pages

import io.appium.java_client.AppiumBy
import io.appium.java_client.android.AndroidDriver
import org.openqa.selenium.By

class AboutScreen(private val driver: AndroidDriver) {

    private val loveIsAllTitle: By =
        AppiumBy.androidUIAutomator("new UiSelector().text(\"Love is all\")")

    fun isPresent(locator: By): Boolean =
        driver.findElements(locator).isNotEmpty()

    fun isQuotesVisible(): Boolean {
        if (isPresent(loveIsAllTitle)) return true
        return driver.findElements(AppiumBy.className("android.widget.TextView"))
            .any { it.isDisplayed && it.text.length > 20 }
    }
}
