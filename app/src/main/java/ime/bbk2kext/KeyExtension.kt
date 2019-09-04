package ime.bbk2kext

import android.inputmethodservice.InputMethodService
import android.os.SystemClock
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.view.HapticFeedbackConstants

class KeyExtension : InputMethodService() {
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
                    metaState = metaState or KeyEvent.META_CTRL_LEFT_ON
                } else if (keyCodeStr.startsWith("S")) {
                    keySequences.add(Pair(KeyEvent.KEYCODE_SHIFT_LEFT, metaState))
                    metaState = metaState or KeyEvent.META_SHIFT_LEFT_ON
                } else if (keyCodeStr.startsWith("A")) {
                    keySequences.add(Pair(KeyEvent.KEYCODE_ALT_LEFT, metaState))
                    metaState = metaState or KeyEvent.META_ALT_LEFT_ON
                } else if (keyCodeStr.startsWith("W")) {
                    keySequences.add(Pair(KeyEvent.KEYCODE_META_LEFT, metaState))
                    metaState = metaState or KeyEvent.META_META_LEFT_ON
                }
                val keyCodeNumStr = keyCodeStr.replace("C", "").replace("S", "").replace("A", "").replace("W", "")
                val keyCode = keyCodeNumStr.toInt()
                keySequences.add(Pair(keyCode, metaState))
                keyButton.setOnTouchListener(object : View.OnTouchListener {
                    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                        if (v == null || event == null) { return false }
                        val ic = currentInputConnection
                        if (event.action == MotionEvent.ACTION_DOWN) {
                            for (k in keySequences) {
                                ic.sendKeyEvent(KeyEvent(event.downTime, event.eventTime, KeyEvent.ACTION_DOWN, k.first, 0, k.second))
                            }
                            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_PRESS)
                            return true
                        } else if (event.action == MotionEvent.ACTION_UP) {
                            for (k in keySequences.reversed()) {
                                ic.sendKeyEvent(KeyEvent(event.downTime, event.eventTime, KeyEvent.ACTION_UP, k.first, 0, k.second))
                            }
                            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_RELEASE)
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

            }
        }
        return keyboard
    }
}
