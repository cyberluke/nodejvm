@file:Suppress("UNUSED_VARIABLE")

package net.plan99.nodejs.kotlin

import net.plan99.nodejs.NodeJS
import org.graalvm.polyglot.TypeLiteral
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.intellij.lang.annotations.Language
import java.awt.SystemColor.info
import java.io.File
import java.util.concurrent.Callable
import java.util.function.Consumer
import kotlin.reflect.KProperty

/**
 * Enters the NodeJS event loop thread and makes the JavaScript API available in scope. You can return JavaScript
 * objects from a nodejs block, however, you can't access them again until you're back inside. Attempting to use
 * them outside a nodejs block will trigger a threading violation exception.
 *
 * Note that this function schedules the lambda onto the NodeJS event loop and waits for it to be executed. If Node
 * is busy, it'll block and wait for it.
 */
fun <T> nodejs(body: NodeJSBlock.() -> T): T = NodeJS.runJS { body(NodeJSBlock()) }

/**
 * Defines various Kotlin helpers to make working with the JavaScript environment more pleasant. Intended to be used
 * inside a [nodejs] block.
 */
class NodeJSBlock internal constructor() {
    /** Uses [Value.as] to convert to the requested type. */
    inline fun <reified T> Value.cast(): T = `as`(object : TypeLiteral<T>() {})

    /** Casts any object to being a JavaScript object. */
    fun Any.asValue(): Value = NodeJS.polyglotContext().asValue(this)

    /**
     * Evaluates the given JavaScript string and casts the result to the desired JVM type. You can request a cast
     * to interfaces that map to JS objects, collections, the Graal/Polyglot [Value] type, boxed primitives and more.
     */
    inline fun <reified T> eval(@Language("JavaScript") javascript: String): T = NodeJS.eval(javascript).cast()

    /**
     * Evaluates the given JavaScript but throws away any result.
     */
    fun run(@Language("JavaScript") javascript: String) {
        NodeJS.eval(javascript)
    }

    /** Allows you to read JS properties of the given [Value] using Kotlin indexing syntax. */
    inline operator fun <reified T> Value.get(key: String): T = getMember(key).cast()

    /** Allows you to read JS properties of the given [Value] using Kotlin indexing syntax. */
    operator fun Value.get(key: String): Value = getMember(key)

    /** Allows you to set JS properties of the given [Value] using Kotlin indexing syntax. */
    operator fun Value.set(key: String, value: Any?) = putMember(key, value)

    private val bindings = NodeJS.polyglotContext().polyglotBindings

    /** Implementation for [bind]. The necessary operator functions are defined as extensions to allow for reified generics. */
    class Binding<T>
    operator fun <T> Binding<T>.setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        // This rather ugly hack is required as we can't just insert the name directly,
        // we have to go via an intermediate 'bindings' map.
        bindings["__nodejvm_transfer"] = value
        NodeJS.eval("${property.name} = Polyglot.import('__nodejvm_transfer');")
        bindings.removeMember("__nodejvm_transfer")
    }
    inline operator fun <reified T> Binding<T>.getValue(thisRef: Any?, property: KProperty<*>): T = eval(property.name)

    inner class Binder<T>(private val default: T? = null) {
        operator fun provideDelegate(thisRef: Any?, prop: KProperty<*>): Binding<T> {
            val b = Binding<T>()
            if (default != null)
                b.setValue(null, prop, default)
            return b
        }
    }

    /**
     * Use this in property delegate syntax to access top level global variables in the NodeJS context. By declaring
     * a variable as `var x: String by bind()` you can read and write the 'x' global variable in JavaScript world.
     */
    fun <T> bind(default: T? = null) = Binder(default)
}