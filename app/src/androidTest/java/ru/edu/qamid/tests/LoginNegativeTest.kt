package ru.edu.qamid.tests

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
import org.junit.Test
import org.junit.runner.RunWith
import ru.edu.qamid.tests.pages.LoginPage
import ru.edu.qamid.tests.pages.MainPage
import java.net.URL

@Epic("Мобильный хоспис")
@Feature("Авторизация")
@RunWith(AllureAndroidJUnit4::class)
class LoginNegativeTest {

    private lateinit var driver: AndroidDriver
    private lateinit var loginPage: LoginPage
    private lateinit var mainPage: MainPage

    @Before
    fun setUp() {
        val options = UiAutomator2Options()
        driver = AndroidDriver(URL("http://10.0.2.2:4723"), options)

        Allure.step("Запуск приложения") {
            driver.activateApp("ru.edu.qamid")
            Thread.sleep(3000)
        }

        loginPage = LoginPage(driver)
        mainPage = MainPage(driver)

        Allure.step("Подготовка: выйти, если уже выполнен вход") {
            if (mainPage.isMainScreen(5)) {
                mainPage.tapAuthorization()
                mainPage.tapLogout()
                Thread.sleep(2000)
            }
        }

        Allure.step("Проверка, что открыт экран авторизации") {
            assertTrue("Экран авторизации не открылся", loginPage.isDisplayed(15))
        }
    }

    @After
    fun tearDown() {
        if (::driver.isInitialized) {
            driver.quit()
        }
    }

    @Test
    @Story("Негативные сценарии")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Вход с неверным логином")
    @Description("Авторизация с несуществующим логином и валидным паролем не открывает главный экран")
    fun testInvalidLogin() {
        Allure.step("Ввести несуществующий логин и валидный пароль, нажать SIGN IN") {
            val stillOnLogin = loginPage.loginWithWrongLogin("wrong_login_123")
            println("Результат: остались на экране входа: $stillOnLogin")
        }
        Allure.step("Проверка: мы не вошли в приложение") {
            assertTrue("Вход с неверным логином не должен был пройти", loginPage.isDisplayed(10))
        }
    }

    @Test
    @Story("Негативные сценарии")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Вход с неверным паролем")
    @Description("Авторизация с валидным логином и неверным паролем не открывает главный экран")
    fun testInvalidPassword() {
        Allure.step("Ввести валидный логин и неверный пароль, нажать SIGN IN") {
            val stillOnLogin = loginPage.loginWith("wrong_password_123")
            println("Результат: остались на экране входа: $stillOnLogin")
        }
        Allure.step("Проверка: мы не вошли в приложение") {
            assertTrue("Вход с неверным паролем не должен был пройти", loginPage.isDisplayed(10))
        }
    }

    @Test
    @Story("Негативные сценарии")
    @DisplayName("Вход с пустыми полями")
    @Description("Попытка входа с пустыми логином и паролем не открывает главный экран")
    fun testEmptyFields() {
        Allure.step("Нажать SIGN IN с пустыми полями") {
            loginPage.tapSignIn()
        }
        Allure.step("Проверка: мы не вошли в приложение") {
            assertTrue("Вход с пустыми полями не должен был пройти", loginPage.isDisplayed(10))
        }
    }

    @Test
    @Story("Позитивные сценарии")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Вход с валидными данными")
    @Description("Авторизация с валидными логином и паролем проходит успешно")
    fun testValidLogin() {
        Allure.step("Ввести валидные логин и пароль, нажать SIGN IN") {
            loginPage.login()
        }
        Allure.step("Проверка: экран авторизации закрылся") {
            val loggedIn = !loginPage.isDisplayed(10)
            println("Результат: экран авторизации закрылся: $loggedIn")
            assertTrue("После валидного входа экран авторизации не закрылся", loggedIn)
        }
    }
}
