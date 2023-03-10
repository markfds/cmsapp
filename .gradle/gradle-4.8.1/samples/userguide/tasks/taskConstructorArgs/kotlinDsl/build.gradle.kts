import org.gradle.api.*
import org.gradle.api.tasks.*
import javax.inject.Inject

open class CustomTask @Inject constructor(private val message: String, private val number: Int) : DefaultTask() {
    @TaskAction fun run() = println("$message $number")
}

tasks.create<CustomTask>("myTask", "hello", 42)
