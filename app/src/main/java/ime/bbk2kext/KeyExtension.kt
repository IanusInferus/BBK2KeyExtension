package ime.bbk2kext

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

    var currentPressedKeys = 0
    var currentPressingKeys = 0
    var modifierState = 0
    var lockState = 0
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
                            currentPressedKeys += 1
                            currentPressingKeys += 1
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
                                return true
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
        return true
    }

    override fun onFinishInput() {
        super.onFinishInput()
        finish()
    }

    fun finish() {
        currentPressedKeys = 0
        currentPressingKeys = 0
        modifierState = 0
        modifierReleasing = false
        for (f in resetModifiers) {
            f()
        }
        releaseModifiers.clear()
    }
}
