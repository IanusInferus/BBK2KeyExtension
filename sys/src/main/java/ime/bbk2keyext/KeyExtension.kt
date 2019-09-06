package ime.bbk2keyext

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.SystemClock
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.LinearLayout
import android.view.inputmethod.InputMethodManager
import kotlin.collections.ArrayList

class KeyExtension : InputMethodService() {
    val logOn = false
    val tag = "BBK2KEXT"

    val modifierKeyToMetaState = hashMapOf(
            Pair(KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON),
            Pair(KeyEvent.KEYCODE_CTRL_RIGHT, KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_RIGHT_ON),
            Pair(KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON),
            Pair(KeyEvent.KEYCODE_SHIFT_RIGHT, KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_RIGHT_ON),
            Pair(KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.META_ALT_ON or KeyEvent.META_ALT_LEFT_ON),
            Pair(KeyEvent.KEYCODE_ALT_RIGHT, KeyEvent.META_ALT_ON or KeyEvent.META_ALT_RIGHT_ON),
            Pair(KeyEvent.KEYCODE_META_LEFT, KeyEvent.META_META_ON or KeyEvent.META_META_LEFT_ON),
            Pair(KeyEvent.KEYCODE_META_RIGHT, KeyEvent.META_META_ON or KeyEvent.META_META_RIGHT_ON)
    )
    val lockKeyToMetaState = hashMapOf(
            Pair(KeyEvent.KEYCODE_CAPS_LOCK, KeyEvent.META_CAPS_LOCK_ON),
            Pair(KeyEvent.KEYCODE_NUM_LOCK, KeyEvent.META_NUM_LOCK_ON),
            Pair(KeyEvent.KEYCODE_SCROLL_LOCK, KeyEvent.META_SCROLL_LOCK_ON)
    )
    //KeyCode to alt mapped keyCode and metaState and scanCode (it can be looked up in /system/usr/keylayout/Generic.kl on an Android device)
    val phyiscalKeyboardAltMapping = hashMapOf(
            Pair(KeyEvent.KEYCODE_Q, Triple(KeyEvent.KEYCODE_POUND, 0, 16)),
            Pair(KeyEvent.KEYCODE_W, Triple(KeyEvent.KEYCODE_1, 0, 2)),
            Pair(KeyEvent.KEYCODE_E, Triple(KeyEvent.KEYCODE_2, 0, 3)),
            Pair(KeyEvent.KEYCODE_R, Triple(KeyEvent.KEYCODE_3, 0, 4)),
            Pair(KeyEvent.KEYCODE_T, Triple(KeyEvent.KEYCODE_9, KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON, 10)),
            Pair(KeyEvent.KEYCODE_Y, Triple(KeyEvent.KEYCODE_0, KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON, 11)),
            Pair(KeyEvent.KEYCODE_U, Triple(KeyEvent.KEYCODE_MINUS, KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON, 12)),
            Pair(KeyEvent.KEYCODE_I, Triple(KeyEvent.KEYCODE_MINUS, 0, 12)),
            Pair(KeyEvent.KEYCODE_O, Triple(KeyEvent.KEYCODE_EQUALS, KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON, 13)),
            Pair(KeyEvent.KEYCODE_P, Triple(KeyEvent.KEYCODE_2, KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON, 3)),
            Pair(KeyEvent.KEYCODE_A, Triple(KeyEvent.KEYCODE_8, KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON, 9)),
            Pair(KeyEvent.KEYCODE_S, Triple(KeyEvent.KEYCODE_4, 0, 5)),
            Pair(KeyEvent.KEYCODE_D, Triple(KeyEvent.KEYCODE_5, 0, 6)),
            Pair(KeyEvent.KEYCODE_F, Triple(KeyEvent.KEYCODE_6, 0, 7)),
            Pair(KeyEvent.KEYCODE_G, Triple(KeyEvent.KEYCODE_SLASH, 0, 53)),
            Pair(KeyEvent.KEYCODE_H, Triple(KeyEvent.KEYCODE_SEMICOLON, KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON, 39)),
            Pair(KeyEvent.KEYCODE_J, Triple(KeyEvent.KEYCODE_SEMICOLON, 0, 39)),
            Pair(KeyEvent.KEYCODE_K, Triple(KeyEvent.KEYCODE_APOSTROPHE, 0, 40)),
            Pair(KeyEvent.KEYCODE_L, Triple(KeyEvent.KEYCODE_APOSTROPHE, KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON, 40)),
            Pair(KeyEvent.KEYCODE_Z, Triple(KeyEvent.KEYCODE_7, 0, 8)),
            Pair(KeyEvent.KEYCODE_X, Triple(KeyEvent.KEYCODE_8, 0, 9)),
            Pair(KeyEvent.KEYCODE_C, Triple(KeyEvent.KEYCODE_9, 0, 10)),
            Pair(KeyEvent.KEYCODE_V, Triple(KeyEvent.KEYCODE_SLASH, KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON, 53)),
            Pair(KeyEvent.KEYCODE_B, Triple(KeyEvent.KEYCODE_1, KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON, 2)),
            Pair(KeyEvent.KEYCODE_N, Triple(KeyEvent.KEYCODE_COMMA, 0, 51)),
            Pair(KeyEvent.KEYCODE_M, Triple(KeyEvent.KEYCODE_PERIOD, 0, 52))
    )

    var virtualKeyboardVisible = true
    var currentPressedKeys = 0
    var currentPressingKeys = 0
    var modifierState = 0
    var lockState = 0
    var physicalAltOn = false
    var modifierReleasing = false
    val resetModifiers = ArrayList<() -> Unit>()
    val releaseModifiers = ArrayList<() -> Unit>()
    var statusIcons = ArrayList<Int>()

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateInputView(): View {
        val keyboard = layoutInflater.inflate(R.layout.keys, null) as LinearLayout
        val handler = Handler()
        val initialInterval: Long = 500
        val normalInterval: Long = 50
        for (rowIndex in 0 until keyboard.childCount) {
            val row = keyboard.getChildAt(rowIndex) as LinearLayout
            for (keyIndex in 0 until row.childCount) {
                val keyButton = row.getChildAt(keyIndex) as Button
                val keyInfo = keyButton.hint?.toString() ?: ""
                if (keyInfo == "") { continue }
                val rKeyInfo = Regex("""(?<Modifiers>[CSAW]*)(?<KeyCode>\d+)-(?<ScanCode>\d+)""")
                val (modifiers, keyCodeStr, scanCodeStr) = rKeyInfo.matchEntire(keyInfo)?.destructured ?: continue
                val keyCode = keyCodeStr.toInt()
                val scanCode = scanCodeStr.toInt()
                var keySequences = ArrayList<Triple<Int, Int, Int>>()
                var metaState = 0
                for (modifier in modifiers) {
                    if (modifier== 'C') {
                        keySequences.add(Triple(KeyEvent.KEYCODE_CTRL_LEFT, metaState, 29))
                        metaState = metaState or KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON
                    } else if (modifier == 'S') {
                        keySequences.add(Triple(KeyEvent.KEYCODE_SHIFT_LEFT, metaState, 42))
                        metaState = metaState or KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON
                    } else if (modifier == 'A') {
                        keySequences.add(Triple(KeyEvent.KEYCODE_ALT_LEFT, metaState, 56))
                        metaState = metaState or KeyEvent.META_ALT_ON or KeyEvent.META_ALT_LEFT_ON
                    } else if (modifier == 'W') {
                        keySequences.add(Triple(KeyEvent.KEYCODE_META_LEFT, metaState, 125))
                        metaState = metaState or KeyEvent.META_META_ON or KeyEvent.META_META_LEFT_ON
                    }
                }
                keySequences.add(Triple(keyCode, metaState, scanCode))
                val isModifier = modifierKeyToMetaState.contains(keyCode)
                val isLock = lockKeyToMetaState.contains(keyCode)
                var repeat: Runnable? = null
                keyButton.setOnTouchListener(object : View.OnTouchListener {
                    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                        if (v == null || event == null) { return false }
                        val ic = currentInputConnection
                        if (event.action == MotionEvent.ACTION_DOWN) {
                            currentPressingKeys += 1
                            currentPressedKeys += 1
                            for (k in keySequences) {
                                val e = KeyEvent(event.downTime, event.eventTime, KeyEvent.ACTION_DOWN, k.first, 0, modifierState or lockState or k.second, KeyCharacterMap.VIRTUAL_KEYBOARD, k.third)
                                if (logOn) { Log.i(tag, "down ${e.downTime} event ${e.eventTime} keyCode ${e.keyCode} scanCode ${e.scanCode} number ${e.number} unicode ${e.unicodeChar} rep ${e.repeatCount} ms ${e.metaState}") }
                                ic.sendKeyEvent(e)
                            }
                            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_PRESS)
                            if (isModifier) {
                                val m = modifierKeyToMetaState[keyCode]!!
                                if ((modifierState and m) != 0) {
                                    modifierState = modifierState and m.inv()
                                    (v as Button).setBackgroundColor(Color.BLACK)
                                    modifierReleasing = true
                                } else {
                                    modifierState = modifierState or m
                                    (v as Button).setBackgroundColor(Color.DKGRAY)
                                }
                            } else if (isLock) {
                                val m = lockKeyToMetaState[keyCode]!!
                                if ((lockState and m) != 0) {
                                    lockState = lockState and m.inv()
                                    (v as Button).setBackgroundColor(Color.BLACK)
                                } else {
                                    lockState = lockState or m
                                    (v as Button).setBackgroundColor(Color.DKGRAY)
                                }
                            } else {
                                var repeatCount = 0
                                repeat = Runnable {
                                    val time = SystemClock.uptimeMillis()
                                    for (k in keySequences) {
                                        val e = KeyEvent(event.downTime, time, KeyEvent.ACTION_DOWN, k.first, repeatCount, modifierState or lockState or k.second, KeyCharacterMap.VIRTUAL_KEYBOARD, k.third)
                                        if (logOn) { Log.i(tag, "down ${e.downTime} event ${e.eventTime} keyCode ${e.keyCode} scanCode ${e.scanCode} number ${e.number} unicode ${e.unicodeChar} rep ${e.repeatCount} ms ${e.metaState}") }
                                        ic.sendKeyEvent(e)
                                    }
                                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_PRESS)
                                    repeatCount += 1
                                    handler.postDelayed(repeat, normalInterval)
                                }
                                handler.postDelayed(repeat, initialInterval)
                            }
                            return true
                        } else if (event.action == MotionEvent.ACTION_UP) {
                            if (repeat != null) {
                                handler.removeCallbacks(repeat)
                                repeat = null
                            }
                            currentPressingKeys -= 1
                            val onRelease: () -> Unit = {
                                if (isModifier) {
                                    modifierState = modifierState and modifierKeyToMetaState[keyCode]!!.inv()
                                    (v as Button).setBackgroundColor(Color.BLACK)
                                }
                                for (k in keySequences.reversed()) {
                                    val e = KeyEvent(event.downTime, event.eventTime, KeyEvent.ACTION_UP, k.first, 0, modifierState or lockState or k.second, KeyCharacterMap.VIRTUAL_KEYBOARD, k.third)
                                    if (logOn) { Log.i(tag, "up ${e.downTime} event ${e.eventTime} keyCode ${e.keyCode} scanCode ${e.scanCode} number ${e.number} unicode ${e.unicodeChar} rep ${e.repeatCount} ms ${e.metaState}") }
                                    ic.sendKeyEvent(e)
                                }
                                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_RELEASE)
                            }
                            if (isModifier && (currentPressedKeys <= 1) && !modifierReleasing) {
                                releaseModifiers.add(onRelease)
                            } else {
                                onRelease()
                                for (r in releaseModifiers.reversed()) {
                                    r()
                                }
                                releaseModifiers.clear()
                            }
                            if (currentPressingKeys == 0) {
                                currentPressedKeys = 0
                                modifierReleasing = false
                            }
                            return true
                        } else if (event.action == MotionEvent.ACTION_CANCEL) {
                            finish()
                            return true
                        }
                        return false
                    }
                })
                if (isModifier) {
                    resetModifiers.add {
                        keyButton.setBackgroundColor(Color.BLACK)
                    }
                }
            }
        }
        return keyboard
    }

    @SuppressLint("MissingSuperCall")
    override fun onEvaluateInputViewShown(): Boolean {
        return virtualKeyboardVisible
    }

    override fun onFinishInput() {
        super.onFinishInput()
        finish()
    }

    fun finish() {
        currentPressedKeys = 0
        currentPressingKeys = 0
        modifierState = 0
        physicalAltOn = false
        modifierReleasing = false
        for (f in resetModifiers) {
            f()
        }
        releaseModifiers.clear()
        statusIcons.clear()
        hideStatusIcon()
    }

    fun updateStatusIcon() {
        if (statusIcons.size > 0) {
            showStatusIcon(statusIcons.last())
        } else {
            hideStatusIcon()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (event == null) { return false }
        val isPhysicalSym = event.keyCode == KeyEvent.KEYCODE_SYM
        val isPhysicalAlt = event.keyCode == KeyEvent.KEYCODE_ALT_LEFT
        val isModifier = modifierKeyToMetaState.contains(keyCode) && !isPhysicalAlt
        val isLock = lockKeyToMetaState.contains(keyCode)
        val ic = currentInputConnection
        if (event.repeatCount == 0) {
            currentPressingKeys += 1
            currentPressedKeys += 1
        }
        if (isPhysicalSym) {
            virtualKeyboardVisible = !isInputViewShown
            updateInputViewShown()
            if (!isInputViewShown) {
                val manager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                @Suppress("DEPRECATION")
                manager.showSoftInputFromInputMethod(window!!.window!!.attributes.token, InputMethodManager.SHOW_FORCED)
            }
        } else if (isPhysicalAlt) {
            if (physicalAltOn) {
                physicalAltOn = false
                statusIcons = ArrayList<Int>(statusIcons.filter { it != R.drawable.palt })
                updateStatusIcon()
                modifierReleasing = true
            } else {
                physicalAltOn = true
                statusIcons.add(R.drawable.palt)
                updateStatusIcon()
            }
            return true
        } else if (physicalAltOn) {
            val p = phyiscalKeyboardAltMapping[event.keyCode] ?: Triple(event.keyCode, 0, event.keyCode)
            val e = KeyEvent(event.downTime, event.eventTime, KeyEvent.ACTION_DOWN, p.first, event.repeatCount, modifierState or lockState or (event.metaState and KeyEvent.META_ALT_MASK.inv()) or p.second, KeyCharacterMap.VIRTUAL_KEYBOARD, p.third, event.flags)
            if (logOn) { Log.i(tag, "down ${e.downTime} event ${e.eventTime} keyCode ${e.keyCode} scanCode ${e.scanCode} number ${e.number} unicode ${e.unicodeChar} rep ${e.repeatCount} ms ${e.metaState}") }
            ic.sendKeyEvent(e)
        } else {
            val e = KeyEvent(event.downTime, event.eventTime, KeyEvent.ACTION_DOWN, event.keyCode, event.repeatCount, modifierState or lockState or event.metaState, KeyCharacterMap.VIRTUAL_KEYBOARD, event.scanCode, event.flags)
            if (logOn) { Log.i(tag, "down ${e.downTime} event ${e.eventTime} keyCode ${e.keyCode} scanCode ${e.scanCode} number ${e.number} unicode ${e.unicodeChar} rep ${e.repeatCount} ms ${e.metaState}") }
            ic.sendKeyEvent(e)
        }
        if (isModifier && (event.repeatCount == 0)) {
            val m = modifierKeyToMetaState[keyCode]!!
            if ((modifierState and m) != 0) {
                modifierState = modifierState and m.inv()
                if (keyCode == KeyEvent.KEYCODE_CTRL_LEFT) {
                    statusIcons = ArrayList<Int>(statusIcons.filter { it != R.drawable.ctrl })
                    updateStatusIcon()
                } else if (keyCode == KeyEvent.KEYCODE_SHIFT_LEFT) {
                    statusIcons = ArrayList<Int>(statusIcons.filter { it != R.drawable.shift })
                    updateStatusIcon()
                }
                modifierReleasing = true
            } else {
                modifierState = modifierState or m
                if (keyCode == KeyEvent.KEYCODE_CTRL_LEFT) {
                    statusIcons.add(R.drawable.ctrl)
                    updateStatusIcon()
                } else if (keyCode == KeyEvent.KEYCODE_SHIFT_LEFT) {
                    statusIcons.add(R.drawable.shift)
                    updateStatusIcon()
                }
            }
        } else if (isLock && (event.repeatCount == 0)) {
            val m = lockKeyToMetaState[keyCode]!!
            if ((lockState and m) != 0) {
                lockState = lockState and m.inv()
            } else {
                lockState = lockState or m
            }
        }
        return true
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (event == null) { return false }
        val isPhysicalSym = event.keyCode == KeyEvent.KEYCODE_SYM
        val isPhysicalAlt = event.keyCode == KeyEvent.KEYCODE_ALT_LEFT
        val isModifier = modifierKeyToMetaState.contains(keyCode) && !isPhysicalAlt
        val ic = currentInputConnection
        currentPressingKeys -= 1
        if (currentPressingKeys < 0) {
            currentPressingKeys = 0 //workaround problems on IME switching, which may result with onKeyUp without onKeyDown
        }
        val onRelease: () -> Unit = {
            if (isModifier) {
                modifierState = modifierState and modifierKeyToMetaState[keyCode]!!.inv()
                if (keyCode == KeyEvent.KEYCODE_CTRL_LEFT) {
                    statusIcons = ArrayList<Int>(statusIcons.filter { it != R.drawable.ctrl })
                    updateStatusIcon()
                } else if (keyCode == KeyEvent.KEYCODE_SHIFT_LEFT) {
                    statusIcons = ArrayList<Int>(statusIcons.filter { it != R.drawable.shift })
                    updateStatusIcon()
                }
            }
            if (isPhysicalSym) {
            } else if (isPhysicalAlt) {
                physicalAltOn = false
                statusIcons = ArrayList<Int>(statusIcons.filter { it != R.drawable.palt })
                updateStatusIcon()
            } else if (physicalAltOn) {
                val p = phyiscalKeyboardAltMapping[event.keyCode] ?: Triple(event.keyCode, 0, event.keyCode)
                val e = KeyEvent(event.downTime, event.eventTime, KeyEvent.ACTION_UP, p.first, event.repeatCount, modifierState or lockState or (event.metaState and KeyEvent.META_ALT_MASK.inv()) or p.second, KeyCharacterMap.VIRTUAL_KEYBOARD, p.third, event.flags)
                Log.i(tag, "up ${e.downTime} event ${e.eventTime} keyCode ${e.keyCode} scanCode ${e.scanCode} number ${e.number} unicode ${e.unicodeChar} rep ${e.repeatCount} ms ${e.metaState}")
                ic.sendKeyEvent(e)
            } else {
                val e = KeyEvent(event.downTime, event.eventTime, KeyEvent.ACTION_UP, event.keyCode, event.repeatCount, modifierState or lockState or event.metaState, KeyCharacterMap.VIRTUAL_KEYBOARD, event.scanCode, event.flags)
                Log.i(tag, "up ${e.downTime} event ${e.eventTime} keyCode ${e.keyCode} scanCode ${e.scanCode} number ${e.number} unicode ${e.unicodeChar} rep ${e.repeatCount} ms ${e.metaState}")
                ic.sendKeyEvent(e)
            }
        }
        if ((isModifier || isPhysicalAlt) && (currentPressedKeys <= 1) && !modifierReleasing) {
            releaseModifiers.add(onRelease)
        } else {
            onRelease()
            for (r in releaseModifiers.reversed()) {
                r()
            }
            releaseModifiers.clear()
        }
        if (currentPressingKeys == 0) {
            currentPressedKeys = 0
            modifierReleasing = false
        }
        return true
    }
}
