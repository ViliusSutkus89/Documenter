package com.viliussutkus89.documenter

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.pressBack
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.viliussutkus89.android.assetextractor.AssetExtractor
import com.viliussutkus89.documenter.ui.MainActivity
import org.junit.*
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
import java.io.IOException

@LargeTest
@RunWith(Parameterized::class)
class InstrumentedTest {

    @Parameterized.Parameter
    lateinit var pdfFile: File

    private var idlingResource: IdlingResource? = null

    companion object {
        @BeforeClass @JvmStatic
        fun extractPDFs() {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            AssetExtractor(instrumentation.context.assets)
                .setNoOverwrite()
                .extract(instrumentation.targetContext.cacheDir, "testFiles")
        }

        @Parameterized.Parameters @JvmStatic
        fun listPDFs(): List<File> {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val extractedToDir = File(instrumentation.targetContext.cacheDir, "testFiles")
            return instrumentation.context.assets.list("testFiles")!!.map {
                File(extractedToDir, it)
            }
        }
    }

    @get:Rule
    val screenshotFailedTestRule = object: TestWatcher() {
        override fun failed(e: Throwable, description: Description) {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val folder = instrumentation.targetContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val screenshotFile = File(folder, description.testClass.simpleName + "-" + description.methodName + ".png")
            val uiDevice = UiDevice.getInstance(instrumentation)
            try {
                uiDevice.takeScreenshot(screenshotFile)
            } catch (err: IOException) {
                err.printStackTrace()
            }
        }
    }

    @Before
    fun setUp() {
        ActivityScenario.launch(MainActivity::class.java).onActivity { activity ->
            idlingResource = activity.idlingResource
            IdlingRegistry.getInstance().register(idlingResource)
        }
        Intents.init()
    }

    @After
    fun tearDown() {
        Intents.release()
        idlingResource?.let { IdlingRegistry.getInstance().unregister(idlingResource) }
    }

    @Test
    fun testAllSuppliedPDFs() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val authority = appContext.packageName + ".instrumentedTestsProvider"
        val uri = FileProvider.getUriForFile(appContext, authority, pdfFile)

        Intents.intending(IntentMatchers.hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWith(
            Instrumentation.ActivityResult(
                Activity.RESULT_OK,
                Intent().setData(uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            )
        )

        onView(withId(R.id.open_button)).perform(click())
        onView(withId(R.id.documentView)).perform(pressBack())
    }
}
