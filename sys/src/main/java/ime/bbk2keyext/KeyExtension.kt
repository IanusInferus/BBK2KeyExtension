package ime.bbk2keyext

import android.graphics.Color
import android.inputmethodservice.InputMethodService
import android.os.SystemClock
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.view.HapticFeedbackConstants

class KeyExtension : InputMethodService() {
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
    val phyiscalKeyboardAltMapping = hashMapOf(
            Pair(KeyEvent.KEYCODE_Q, Pair(KeyEvent.KEYCODE_POUND, 0)),
            Pair(KeyEvent.KEYCODE_W, Pair(KeyEvent.KEYCODE_1, 0)),
            Pair(KeyEvent.KEYCODE_E, Pair(KeyEvent.KEYCODE_2, 0)),
            Pair(KeyEvent.KEYCODE_R, Pair(KeyEvent.KEYCODE_3, 0)),
            Pair(KeyEvent.KEYCODE_T, Pair(KeyEvent.KEYCODE_9, KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON)),
            Pair(KeyEvent.KEYCODE_Y, Pair(KeyEvent.KEYCODE_0, KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON)),
            Pair(KeyEvent.KEYCODE_U, Pair(KeyEvent.KEYCODE_MINUS, KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON)),
            Pair(KeyEvent.KEYCODE_I, Pair(KeyEvent.KEYCODE_MINUS, 0)),
            Pair(KeyEvent.KEYCODE_O, Pair(KeyEvent.KEYCODE_EQUALS, KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON)),
            Pair(KeyEvent.KEYCODE_P, Pair(KeyEvent.KEYCODE_2, KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON)),
            Pair(KeyEvent.KEYCODE_A, Pair(KeyEvent.KEYCODE_8, KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON)),
            Pair(KeyEvent.KEYCODE_S, Pair(KeyEvent.KEYCODE_4, 0)),
            Pair(KeyEvent.KEYCODE_D, Pair(KeyEvent.KEYCODE_5, 0)),
            Pair(KeyEvent.KEYCODE_F, Pair(KeyEvent.KEYCODE_6, 0)),
            Pair(KeyEvent.KEYCODE_G, Pair(KeyEvent.KEYCODE_SLASH, 0)),
            Pair(KeyEvent.KEYCODE_H, Pair(KeyEvent.KEYCODE_SEMICOLON, KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON)),
            Pair(KeyEvent.KEYCODE_J, Pair(KeyEvent.KEYCODE_SEMICOLON, 0)),
            Pair(KeyEvent.KEYCODE_K, Pair(KeyEvent.KEYCODE_APOSTROPHE, 0)),
            Pair(KeyEvent.KEYCODE_L, Pair(KeyEvent.KEYCODE_APOSTROPHE, KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON)),
            Pair(KeyEvent.KEYCODE_Z, Pair(KeyEvent.KEYCODE_7, 0)),
            Pair(KeyEvent.KEYCODE_X, Pair(KeyEvent.KEYCODE_8, 0)),
            Pair(KeyEvent.KEYCODE_C, Pair(KeyEvent.KEYCODE_9, 0)),
            Pair(KeyEvent.KEYCODE_V, Pair(KeyEvent.KEYCODE_SLASH, KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON)),
            Pair(KeyEvent.KEYCODE_B, Pair(KeyEvent.KEYCODE_1, KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON)),
            Pair(KeyEvent.KEYCODE_N, Pair(KeyEvent.KEYCODE_COMMA, 0)),
            Pair(KeyEvent.KEYCODE_M, Pair(KeyEvent.KEYCODE_PERIOD, 0))
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

    override fun onCreateInputView(): View {
        val keyboard = layoutInflater.inflate(R.layout.keys, null) as LinearLayout
        for (rowIndex in 0 until keyboard.childCount) {
            val row = keyboard.getChildAt(rowIndex) as LinearLayout
            for (keyIndex in 0 until row.childCount) {
                val keyButton = row.getChildAt(keyIndex) as Button
                val keyCodeStr = keyButton.hint?.toString() ?: "62"
                var keySequences = ArrayList<Pair<Int, Int>>()
                var metaState = 0
                if (keyCodeStr.startsWith("C")) {
                    keySequences.add(Pair(KeyEvent.KEYCODE_CTRL_LEFT, metaState))
                    metaState = metaState or KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON
                } else if (keyCodeStr.startsWith("S")) {
                    keySequences.add(Pair(KeyEvent.KEYCODE_SHIFT_LEFT, metaState))
                    metaState = metaState or KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON
                } else if (keyCodeStr.startsWith("A")) {
                    keySequences.add(Pair(KeyEvent.KEYCODE_ALT_LEFT, metaState))
                    metaState = metaState or KeyEvent.META_ALT_ON or KeyEvent.META_ALT_LEFT_ON
                } else if (keyCodeStr.startsWith("W")) {
                    keySequences.add(Pair(KeyEvent.KEYCODE_META_LEFT, metaState))
                    metaState = metaState or KeyEvent.META_META_ON or KeyEvent.META_META_LEFT_ON
                }
                val keyCodeNumStr = keyCodeStr.replace("C", "").replace("S", "").replace("A", "").replace("W", "")
                val keyCode = keyCodeNumStr.toInt()
                keySequences.add(Pair(keyCode, metaState))
                val isModifier = modifierKeyToMetaState.contains(keyCode)
                val isLock = lockKeyToMetaState.contains(keyCode)
                keyButton.setOnTouchListener(object : View.OnTouchListener {
                    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                        if (v == null || event == null) { return false }
                        val ic = currentInputConnection
                        if (event.action == MotionEvent.ACTION_DOWN) {
                            currentPressingKeys += 1
                            currentPressedKeys += 1
                            for (k in keySequences) {
                                ic.sendKeyEvent(KeyEvent(event.downTime, event.eventTime, KeyEvent.ACTION_DOWN, k.first, 0, modifierState or lockState or k.second))
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
                            }
                            return true
                        } else if (event.action == MotionEvent.ACTION_UP) {
                            currentPressingKeys -= 1
                            val onRelease: () -> Unit = {
                                if (isModifier) {
                                    modifierState = modifierState and modifierKeyToMetaState[keyCode]!!.inv()
                                    (v as Button).setBackgroundColor(Color.BLACK)
                                }
                                for (k in keySequences.reversed()) {
                                    ic.sendKeyEvent(KeyEvent(event.downTime, event.eventTime, KeyEvent.ACTION_UP, k.first, 0, modifierState or lockState or k.second))
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
                keyButton.setOnClickListener(object : View.OnClickListener {
                    override fun onClick(v: View?) {
                        if (v == null) { return }
                        val ic = currentInputConnection
                        val time = SystemClock.uptimeMillis()
                        for (k in keySequences) {
                            ic.sendKeyEvent(KeyEvent(time, time, KeyEvent.ACTION_DOWN, k.first, 0, k.second))
                        }
                        v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_PRESS)
                        for (k in keySequences.reversed()) {
                            ic.sendKeyEvent(KeyEvent(time, time, KeyEvent.ACTION_UP, k.first, 0, k.second))
                        }
                        v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_RELEASE)
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

    override fun onEvaluateInputViewShown(): Boolean {
        super.onEvaluateInputViewShown()
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
            virtualKeyboardVisible = !virtualKeyboardVisible
            updateInputViewShown()
        } else if (isPhysicalAlt) {
            if (physicalAltOn) {
                physicalAltOn = false
                //(v as Button).setBackgroundColor(Color.BLACK)
                modifierReleasing = true
            } else {
                physicalAltOn = true
                //(v as Button).setBackgroundColor(Color.DKGRAY)
            }
            return true
        } else if (physicalAltOn) {
            val p = phyiscalKeyboardAltMapping[event.keyCode] ?: Pair(event.keyCode, 0)
            ic.sendKeyEvent(KeyEvent(event.downTime, event.eventTime, KeyEvent.ACTION_DOWN, p.first, event.repeatCount, modifierState or lockState or (event.metaState and KeyEvent.META_ALT_MASK.inv()) or p.second))
        } else {
            ic.sendKeyEvent(KeyEvent(event.downTime, event.eventTime, KeyEvent.ACTION_DOWN, event.keyCode, event.repeatCount, modifierState or lockState or event.metaState))
        }
        if (isModifier) {
            val m = modifierKeyToMetaState[keyCode]!!
            if ((modifierState and m) != 0) {
                modifierState = modifierState and m.inv()
                //(v as Button).setBackgroundColor(Color.BLACK)
                modifierReleasing = true
            } else {
                modifierState = modifierState or m
                //(v as Button).setBackgroundColor(Color.DKGRAY)
            }
        } else if (isLock) {
            val m = lockKeyToMetaState[keyCode]!!
            if ((lockState and m) != 0) {
                lockState = lockState and m.inv()
                //(v as Button).setBackgroundColor(Color.BLACK)
            } else {
                lockState = lockState or m
                //(v as Button).setBackgroundColor(Color.DKGRAY)
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
                //(v as Button).setBackgroundColor(Color.BLACK)
            }
            if (isPhysicalSym) {
            } else if (isPhysicalAlt) {
                physicalAltOn = false
                //(v as Button).setBackgroundColor(Color.BLACK)
            } else if (physicalAltOn) {
                val p = phyiscalKeyboardAltMapping[event.keyCode] ?: Pair(event.keyCode, 0)
                ic.sendKeyEvent(KeyEvent(event.downTime, event.eventTime, KeyEvent.ACTION_UP, p.first, event.repeatCount, modifierState or lockState or (event.metaState and KeyEvent.META_ALT_MASK.inv()) or p.second))
            } else {
                ic.sendKeyEvent(KeyEvent(event.downTime, event.eventTime, KeyEvent.ACTION_UP, event.keyCode, event.repeatCount, modifierState or lockState or event.metaState))
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
        return super.onKeyUp(keyCode, event)
    }
}
