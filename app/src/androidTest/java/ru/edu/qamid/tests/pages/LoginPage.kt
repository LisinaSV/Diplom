package ru.edu.qamid.tests.pages

import io.appium.java_client.AppiumBy
import io.appium.java_client.android.AndroidDriver
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import ru.edu.qamid.tests.TestData

class LoginPage(private val driver: AndroidDriver) {

    private val loginField: By =
        AppiumBy.androidUIAutomator(
            "new UiSelector().className(\"android.widget.EditText\").instance(0)"
        )
    private val passwordField: By =
        AppiumBy.androidUIAutomator(
            "new UiSelector().className(\"android.widget.EditText\").instance(1)"
        )

    fun waitFor(locator: By, timeoutSeconds: Long = 20): WebElement {
        val deadline = System.currentTimeMillis() + timeoutSeconds * 1000
        var lastError: Exception? = null
        while (System.currentTimeMillis() < deadline) {
            try {
                return driver.findElement(locator)
            } catch (e: Exception) {
                lastError = e
                Thread.sleep(500)
            }
        }
        throw lastError ?: IllegalStateException("Элемент не найден: $locator")
    }

    fun isDisplayed(timeoutSeconds: Long = 15): Boolean = try {
        waitFor(loginField, timeoutSeconds)
        true
    } catch (e: Exception) {
        false
    }

    fun tapSignIn() {
        val button: WebElement = try {
            waitFor(AppiumBy.androidUIAutomator("new UiSelector().text(\"SIGN IN\")"))
        } catch (e: Exception) {
            waitFor(AppiumBy.androidUIAutomator("new UiSelector().text(\"Войти\")"))
        }
        button.click()
        Thread.sleep(2500)
    }

    fun login(): Boolean {
        if (!isDisplayed()) return false
        val fields = driver.findElements(loginField)
        val passwords = driver.findElements(passwordField)
        fields[0].sendKeys(TestData.LOGIN)
        passwords[0].sendKeys(TestData.PASSWORD)
        tapSignIn()
        return true
    }

    fun loginWith(password: String): Boolean {
        if (!isDisplayed()) return false
        val fields = driver.findElements(loginField)
        val passwords = driver.findElements(passwordField)
        fields[0].clear()
        fields[0].sendKeys(TestData.LOGIN)
        passwords[0].clear()
        passwords[0].sendKeys(password)
        tapSignIn()
        return !isDisplayed(5)
    }

    fun loginWithWrongLogin(login: String): Boolean {
        if (!isDisplayed()) return false
        val fields = driver.findElements(loginField)
        val passwords = driver.findElements(passwordField)
        fields[0].clear()
        fields[0].sendKeys(login)
        passwords[0].clear()
        passwords[0].sendKeys(TestData.PASSWORD)
        tapSignIn()
        return !isDisplayed(5)
    }
}
