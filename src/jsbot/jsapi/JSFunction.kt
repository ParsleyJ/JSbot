package jsbot.jsapi

import jsbot.JSBot
import org.mozilla.javascript.BaseFunction
import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable

/**
 * Created on 28/09/2019.
 *
 */
open class JSFunction(
    private val metaArity: Int,
    private val func: (Context, Scriptable, Scriptable, Array<out Any>) -> Any?
) : BaseFunction() {
    override fun call(cx: Context, scope: Scriptable, thisObj: Scriptable, args: Array<out Any>): Any? {
        return func.invoke(cx, scope, thisObj, args)
    }

    override fun getArity(): Int {
        return metaArity
    }


    open class JSMonofunction<T1>(func: (Context, Scriptable, Scriptable, T1) -> Any?) : JSFunction(1, f@{
        cx, scope, thisObj, args ->
        if(args.isEmpty()){
            throw JSBot.JSBotException("Missing argument")
        }
        val t = try {
            args[0] as T1
        } catch (cce: ClassCastException) {
            throw JSBot.JSBotException("Invalid argument type")
        }
        return@f func.invoke(cx, scope, thisObj, t)
    })

}