package com.moriak.schednote.adapters

import android.content.Context.INPUT_METHOD_SERVICE
import android.text.*
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.moriak.schednote.App
import com.moriak.schednote.R
import com.moriak.schednote.database.data.Note
import com.moriak.schednote.database.data.NoteCategory
import com.moriak.schednote.database.data.Subject
import com.moriak.schednote.other.TimeCategory
import com.moriak.schednote.settings.Prefs
import kotlinx.android.synthetic.main.readable_note.view.*
import kotlinx.android.synthetic.main.writable_note.view.*
import java.util.*

/**
 * V adaptéri zobrazujem mením a pridávam poznámky
 */
class NoteAdapter(private var category: NoteCategory, private val subjects: ArrayList<Subject>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val items = ArrayList<Note>()

    private var noSubject: Subject = Subject(-1L, "NONE", App.str(R.string.no_subjects))
    private val ePos: Int
        get() = when {
            subjects.isEmpty() -> -1
            eId == -1L -> items.size
            eId < -1L -> -1
            else -> items.indexOfFirst { it.id == eId }
        }
    private val eErr: Int
        get() = when {
            eSub == noSubject -> R.string.no_subjects
            eInfo.isEmpty() -> R.string.note_no_description
            eInfo.length > Note.limit -> R.string.note_description_length_exceeded
            !eInfo.matches(Note.validFormat) -> R.string.note_invalid_format
            eDate?.let { it <= System.currentTimeMillis() } == true -> R.string.time_out
            else -> 0
        }
    private var eCur: IntRange = 0..0
    private var eId: Long = -1L
    private var eSub: Subject = noSubject
    private var eDate: Long? = null
    private var eInfo: String = ""

    init {
        determineSubject()
    }

    private val onEdit = View.OnClickListener { performEdit(holderPos(it)) }
    private val onSave = View.OnClickListener {
        (it.tag as WritableNoteHolder).focus = false
        performEdit(items.size)
    }
    private val onLose = View.OnClickListener {
        val pos = ePos
        if (pos == items.size) initialize() //pri vkladani by cancel hodnoty nevynuloval
        performEdit(items.size, pos < items.size)
    }
    private val onRemove = View.OnClickListener { removeItem(holderPos(it)) }
    private var postDateTimeRequest = fun(_: Long?) = Unit
    private val onPrevSub = View.OnClickListener { switchSubject(it, -1) }
    private val onNextSub = View.OnClickListener { switchSubject(it, 1) }
    private val onDateTimeRequest = View.OnClickListener { postDateTimeRequest(eDate) }
    private val onDateClr = View.OnClickListener { setDeadline(null) }
    private val onFocusChange = View.OnFocusChangeListener(this::switchKeyboard)
    private val cursorWatcher = object : SpanWatcher {
        override fun onSpanAdded(text: Spannable?, what: Any?, start: Int, end: Int) {}
        override fun onSpanRemoved(text: Spannable?, what: Any?, start: Int, end: Int) {}
        override fun onSpanChanged(s: Spannable?, o: Any?, os: Int, oe: Int, ns: Int, ne: Int) {
            eCur = Selection.getSelectionStart(s)..Selection.getSelectionEnd(s)
        }
    }
    private val onRewrite = object : TextWatcher {
        private var st = 0
        private var en = 0
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            st = start
            en = start + count
        }

        override fun afterTextChanged(s: Editable?) {
            s ?: return
            if (!s.contains(Note.validFormat)) s.delete(st, en)
            if (!s.contains(Note.validFormat)) s.clear()
            eCur = Selection.getSelectionStart(s)..Selection.getSelectionEnd(s)
            eInfo = s.toString()
            s.setSpan(cursorWatcher, 0, s.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
        }
    }

    private fun switchKeyboard(v: View?, visible: Boolean) {
        v?.rootView ?: return
        val imm = App.ctx.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(v, 0)
        if (!visible) imm.hideSoftInputFromWindow(v.windowToken, 0)
    }

    /**
     * Nastavenie správania, čo sa má stať pri žiadosti o nastavenie dátumu
     * @param fn Metóda s jedným vstupným parametrom, v ktorom je pôvodný dátum v ms.
     */
    fun setOnDateTimeRequest(fn: (Long?) -> Unit) {
        postDateTimeRequest = fn
    }

    private fun holderPos(v: View) =
        v.tag?.let { if (it is AnyNoteHolder) it else null }?.adapterPosition ?: -1

    private fun determineSubject(): Subject = when {
        category is Subject -> category as Subject
        eSub != noSubject -> eSub
        subjects.isNotEmpty() -> subjects.first()
        else -> noSubject
    }

    private fun initialize(pos: Int = -1) {
        val item = if (pos in items.indices) items[pos] else null
        eId = item?.id ?: -1L
        eSub = item?.sub ?: determineSubject()
        eDate = item?.deadline
        eInfo = item?.description ?: ""
        eCur = eInfo.length.let { it..it }
    }

    private fun performEdit(newPos: Int, cancel: Boolean = false) {
        val oldPos = ePos
        App.log("finish $oldPos, start $newPos, list: $items")
        if (newPos in 0..items.size) when (oldPos) {
            -1 -> {
                App.toast("just editing")
                initialize(newPos)
                notifyItemChanged(newPos)
            }
            newPos -> {
                val new = newPos == items.size
                /*(zabranit vlozeniu / rebind), vložiť, pôvodné hodnoty, chyba */
                when {
                    new && cancel || !new && !cancel -> notifyItemChanged(newPos)
                    new && !cancel -> when {
                        saveItem() -> {
                            initialize()
                            notifyItemChanged(newPos)
                            notifyItemInserted(items.size)
                            removeOutcast(newPos)?.let(App::toast) ?: move(newPos)
                        }
                        eErr > 0 -> App.toast(eErr)
                        else -> App.log("Trouble inserting note in NoteAdapter!")
                    }
                    !new && cancel -> {
                        initialize(newPos)
                        notifyItemChanged(newPos)
                    }
                    else -> throw Exception("Strange behaviour in NoteAdapter::performEdit!")
                }
            }
            else -> {
                val doneInserting = oldPos == items.size
                val goneInserting = newPos == items.size

                // ulozit sucasne upravovany zaznam
                val saved = !cancel && saveItem()
                val err = if (cancel) 0 else eErr
                initialize(newPos)

                when {
                    doneInserting -> {
                        App.toast("done inserting")
                        if (saved) {
                            notifyItemChanged(oldPos)
                            removeOutcast(oldPos)?.also(App::toast) ?: move(oldPos)
                        } else {
                            notifyItemRemoved(oldPos)
                            if (err > 0 && eInfo == "" && eDate == null) App.toast(err)
                        }
                        notifyItemChanged(newPos)
                    }
                    goneInserting -> {
                        App.toast("gone inserting")
                        when {
                            saved || cancel -> {
                                notifyItemChanged(oldPos)
                                notifyItemInserted(newPos)
                                removeOutcast(oldPos)?.also(App::toast) ?: move(oldPos)
                            }
                            err > 0 -> App.toast(err)
                            else -> App.log("Trouble updating note in NoteAdapter!")
                        }
                    }
                    else -> {
                        App.toast("not inserting")
                        notifyItemChanged(oldPos)
                        notifyItemChanged(newPos)
                        if (!saved) App.toast(err)
                        else removeOutcast(oldPos)?.also(App::toast) ?: move(oldPos)
                    }
                }
            }
        }
    }

    private fun saveItem(): Boolean {
        if (eErr > 0) return false
        val id = App.data.setNote(eId, eSub.id, eInfo, eDate)
        if (id > -1L) {
            val note = Note(id, eSub, eInfo, eDate)
            when (val pos = ePos) {
                in items.indices -> items[pos] = note
                items.size -> items.add(note)
                else -> return false
            }
        }
        return id > -1L
    }

    private fun removeItem(pos: Int): Boolean = when (pos) {
        in items.indices -> true.also {
            App.data.removeNote(items[pos].id)
            items.removeAt(pos)
            notifyItemRemoved(pos)
        }
        items.size -> true.also { performEdit(pos) }
        else -> false
    }

    fun setDeadline(deadline: Long?) {
        if (deadline != null && deadline < System.currentTimeMillis()) App.toast(R.string.time_out)
        else {
            eDate = deadline
            val pos = ePos
            performEdit(pos, pos == items.size)
        }
    }

    private fun switchSubject(d: Int): String {
        if (subjects.isEmpty()) {
            eSub = noSubject
            return "*"
        }
        var index = (subjects.indexOfFirst { it == eSub } + d) % subjects.size
        if (index < 0) index += subjects.size
        eSub = subjects[index]
        val digits = "%0${"${subjects.size}".length}d"
        return String.format("$digits / $digits", index + 1, subjects.size)
    }

    private fun switchSubject(v: View, d: Int) {
        val count = switchSubject(d)
        val sub = v.tag as TextView
        sub.text = eSub.abb
        (sub.tag as TextView).text = count
    }

    private lateinit var onDateTimeSetAttempt: (Int, Long?) -> Unit

    /**
     * Nastavenie správania, čo sa má stať pri pokuse o zmenu dátumu
     * @param fn Metóda prijíma vstupy: Pozícia položky zoznamu [Int] a id úlohy [Long],
     * ktoré môže byť null, ak práve vytváram novú úlohu
     */
    fun setOnDateTimeSetAttempt(fn: (Int, Long?) -> Unit) {
        onDateTimeSetAttempt = fn
    }

    /**
     * Odstránenie poznámky
     * @param position pozícia na ktorej sa poznámka nachádza
     */

    fun removeOutcast(position: Int): Int? {
        if (position !in items.indices) return null
        val item = items[position]
        val belongs = when (category) {
            TimeCategory.ALL -> true
            TimeCategory.TIMELESS -> item.deadline == null
            TimeCategory.LATE -> item.deadline?.let { item.deadline < System.currentTimeMillis() } == true
            TimeCategory.TODAY, TimeCategory.TOMORROW, TimeCategory.IN_WEEK -> item.deadline?.let {
                val cal = Calendar.getInstance()
                cal.set(
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH) + 1,
                    0,
                    0,
                    0
                )
                cal.set(Calendar.MILLISECOND, 0)
                val midnight = cal.timeInMillis
                val dif = when (category) {
                    TimeCategory.IN_WEEK -> 6
                    TimeCategory.TOMORROW -> 1
                    else -> 0
                }
                if (dif > 0) {
                    cal.add(Calendar.DAY_OF_YEAR, dif)
                    it in midnight until cal.timeInMillis
                } else it in System.currentTimeMillis() until midnight
            } == true
            is Subject -> category == item.sub
            else -> true
        }
        if (belongs) return null
        items.removeAt(position)
        notifyItemRemoved(position)
        return R.string.outcast_note
    }

    private fun move(oldPos: Int) {
        if (items.size <= 1 || oldPos !in items.indices) return
        var newPos = items.indexOfFirst { it > items[oldPos] }
        if (newPos == -1) newPos = items.lastIndex
        items.add(newPos, items.removeAt(oldPos))
        notifyItemMoved(oldPos, newPos)
    }

    /**
     * Odstrániť všetky poznámky patriace pod súčasne vybranú kategóriu
     */
    fun clear() {
        val oldCount = items.size
        if (ePos in items.indices) performEdit(oldCount)
        App.data.clearNotesOfCategory(category)
        items.clear()
        notifyItemRangeRemoved(0, oldCount)
    }

    /**
     * Načítanie úloh patriacich pod vybranú kategóriu
     * @param cat vybraná kategória
     */
    fun loadCategory(cat: NoteCategory) {
        if (category != cat) initialize()
        items.clear()
        category = cat
        items.addAll(App.data.notes(cat))
        notifyDataSetChanged()
    }

    /**
     * Opätovné načítanie zoznamu úloh
     */
    fun reload() {
        items.clear()
        items.addAll(App.data.notes(category))
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int) = when {
        position !in 0..items.size -> super.getItemViewType(position)
        position == items.size || items[position].id == eId -> R.layout.writable_note
        else -> R.layout.readable_note
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(viewType, parent, false)
        return when (viewType) {
            R.layout.readable_note -> ReadableNoteHolder(v)
            R.layout.writable_note -> WritableNoteHolder(v)
            else -> throw Exception("No such ViewType in NoteAdapter!")
        }
    }

    override fun getItemCount() =
        if (subjects.isEmpty()) 0 else items.size + if (eId == -1L) 1 else 0

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) =
        (holder as AnyNoteHolder).bind()

    override fun getItemId(position: Int): Long =
        if (position in items.indices) items[position].id else -1L

    /**
     * Získanie pozície úlohy s danym id
     * @param id
     */
    fun indexOfNote(id: Long) =
        if (id == -1L) items.lastIndex else items.indexOfFirst { it.id == id }

    /**
     * Opätovné načítanie množiny predmetov
     */
    fun reloadSubjects() {
        subjects.clear()
        subjects.addAll(App.data.subjects())
    }

    abstract inner class AnyNoteHolder(view: View) : RecyclerView.ViewHolder(view) {
        protected abstract val sub: TextView
        protected abstract val info: TextView
        protected abstract val date: TextView
        abstract fun bind()
    }

    inner class ReadableNoteHolder(view: View) : AnyNoteHolder(view) {
        override val sub = itemView.rn_sub!!
        override val info = itemView.rn_text!!
        override val date = itemView.rn_date!!
        private val edit = itemView.rn_edit!!
        private val del = itemView.rn_del!!

        init {
            edit.setOnClickListener(onEdit)
            del.setOnClickListener(onRemove)
            sub.setOnClickListener(onEdit)
            date.setOnClickListener(onEdit)
            info.setOnClickListener(onEdit)
            edit.tag = this
            del.tag = this
            sub.tag = this
            date.tag = this
            info.tag = this
        }

        override fun bind() {
            val item = if (adapterPosition in items.indices) items[adapterPosition] else null
            sub.text = item?.sub?.abb ?: eSub.abb
            date.text = (item?.deadline ?: eDate)?.let(Prefs.settings::getDateTimeString) ?: ""
            info.text = item?.description ?: eInfo
        }
    }

    inner class WritableNoteHolder(view: View) : AnyNoteHolder(view) {
        override val sub = itemView.wn_sub!!
        override val info = itemView.wn_text!!
        override val date = itemView.wn_date!!
        private val counter = itemView.wn_sub_counter!!
        private val prev = itemView.wn_prev!!
        private val next = itemView.wn_next!!
        private val noDate = itemView.wn_date_clr!!
        private val save = itemView.wn_save!!
        private val cancel = itemView.wn_cancel!!
        private var sel: IntRange
            get() = info.text?.let { Selection.getSelectionStart(it)..Selection.getSelectionEnd(it) }
                ?: 0..0
            set(value) {
                val rng = 0..(info.text?.length ?: 0)
                Selection.setSelection(
                    info.text,
                    value.first.coerceIn(rng),
                    value.last.coerceIn(rng)
                )
            }
        var focus: Boolean
            get() = info.isFocused
            set(value) {
                if (info.isFocused) info.clearFocus() else info.requestFocus()
                if (value != info.isFocused) if (value) info.requestFocus() else info.clearFocus()
            }

        init {
            prev.tag = sub
            next.tag = sub
            sub.tag = counter
            info.tag = this
            date.tag = this
            noDate.tag = this
            save.tag = this
            cancel.tag = this

            save.setOnClickListener(onSave)
            cancel.setOnClickListener(onLose)
            prev.setOnClickListener(onPrevSub)
            next.setOnClickListener(onNextSub)
            date.setOnClickListener(onDateTimeRequest)
            noDate.setOnClickListener(onDateClr)
            val cur = eCur
            info.addTextChangedListener(onRewrite)
            info.onFocusChangeListener = onFocusChange
            eCur = cur
        }

        override fun bind() {
            val pCur = eCur
            val pInfo = eInfo
            counter.text = switchSubject(0)
            sub.text = eSub.abb
            date.text = eDate?.let(Prefs.settings::getDateTimeString) ?: ""
            info.setText(pInfo)
            info.text!!.setSpan(cursorWatcher, 0, pInfo.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
            eCur = pCur
            sel = eCur
            focus =
                adapterPosition in items.indices || adapterPosition != -1 && (eInfo != "" || eDate != null)
        }
    }
}