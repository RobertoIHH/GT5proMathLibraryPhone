
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class FormulasAdapter : RecyclerView.Adapter<FormulasAdapter.FormulaViewHolder>() {

    private var formulas = listOf<MathLibraryActivity.MathFormula>()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    var onFormulaClickListener: ((MathLibraryActivity.MathFormula) -> Unit)? = null
    var onFavoriteClickListener: ((MathLibraryActivity.MathFormula) -> Unit)? = null
    var onEditClickListener: ((MathLibraryActivity.MathFormula) -> Unit)? = null
    var onDeleteClickListener: ((MathLibraryActivity.MathFormula) -> Unit)? = null

    class FormulaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvFormulaTitle)
        val tvContent: TextView = itemView.findViewById(R.id.tvFormulaContent)
        val tvCategory: TextView = itemView.findViewById(R.id.tvFormulaCategory)
        val tvType: TextView = itemView.findViewById(R.id.tvFormulaType)
        val tvDescription: TextView = itemView.findViewById(R.id.tvFormulaDescription)
        val tvDate: TextView = itemView.findViewById(R.id.tvFormulaDate)
        val btnFavorite: ImageButton = itemView.findViewById(R.id.btnFavorite)
        val btnEdit: ImageButton = itemView.findViewById(R.id.btnEdit)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
        val tvTags: TextView = itemView.findViewById(R.id.tvFormulaTags)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FormulaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_formula, parent, false)
        return FormulaViewHolder(view)
    }

    override fun onBindViewHolder(holder: FormulaViewHolder, position: Int) {
        val formula = formulas[position]

        holder.tvTitle.text = formula.title
        holder.tvDescription.text = formula.description
        holder.tvCategory.text = formula.category
        holder.tvDate.text = dateFormat.format(Date(formula.dateModified))

        // Contenido truncado
        val truncatedContent = if (formula.content.length > 80) {
            "${formula.content.take(80)}..."
        } else {
            formula.content
        }
        holder.tvContent.text = truncatedContent

        // Tipo con icono
        holder.tvType.text = when (formula.type) {
            "latex" -> "📐 LaTeX"
            "markdown" -> "📝 MD"
            "plain" -> "📄 Texto"
            else -> "❓ Otro"
        }

        // Tags
        if (formula.tags.isNotEmpty()) {
            val tagsText = formula.tags.take(3).joinToString(" • ") { "#$it" }
            holder.tvTags.text = tagsText
            holder.tvTags.visibility = View.VISIBLE
        } else {
            holder.tvTags.visibility = View.GONE
        }

        // Botón favorito
        holder.btnFavorite.setImageResource(
            if (formula.isFavorite) android.R.drawable.btn_star_big_on
            else android.R.drawable.btn_star_big_off
        )
        holder.btnFavorite.setOnClickListener {
            onFavoriteClickListener?.invoke(formula)
        }

        // Botón editar
        holder.btnEdit.setImageResource(android.R.drawable.ic_menu_edit)
        holder.btnEdit.setOnClickListener {
            onEditClickListener?.invoke(formula)
        }

        // Botón eliminar
        holder.btnDelete.setImageResource(android.R.drawable.ic_menu_delete)
        holder.btnDelete.setOnClickListener {
            onDeleteClickListener?.invoke(formula)
        }

        // Click en todo el item
        holder.itemView.setOnClickListener {
            onFormulaClickListener?.invoke(formula)
        }

        // Fondo según favorito
        if (formula.isFavorite) {
            holder.itemView.setBackgroundResource(R.drawable.bg_favorite_formula)
        } else {
            holder.itemView.setBackgroundResource(R.drawable.bg_normal_formula)
        }

        // Animación táctil
        holder.itemView.setOnTouchListener { view, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    view.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start()
                }
                android.view.MotionEvent.ACTION_UP,
                android.view.MotionEvent.ACTION_CANCEL -> {
                    view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
                }
            }
            false
        }
    }

    override fun getItemCount(): Int = formulas.size

    fun updateFormulas(newFormulas: List<MathLibraryActivity.MathFormula>) {
        formulas = newFormulas
        notifyDataSetChanged()
    }

    fun getFormula(position: Int): MathLibraryActivity.MathFormula? {
        return if (position in formulas.indices) formulas[position] else null
    }
}