package com.itmbu.itmleadsquare.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.itmbu.itmleadsquare.R
import com.itmbu.itmleadsquare.model.LeadsList
import com.itmbu.itmleadsquare.model.NewLeadsList

class LeadsDisplayAdapter(
    private val context: Context,
    private val categoryList: ArrayList<NewLeadsList>
) : RecyclerView.Adapter<LeadsDisplayAdapter.CategoryViewHolder>() {

    private var filteredList: ArrayList<NewLeadsList> = ArrayList(categoryList)

    interface OnCategoryItemClickListener {
        fun onCategoryItemClick(leads: NewLeadsList)
    }
    var onCategoryItemClickListener: OnCategoryItemClickListener? = null

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.textName)
        private val mobTextView: TextView = itemView.findViewById(R.id.textMobile)
        private val courseTextView: TextView = itemView.findViewById(R.id.textCourse)
        private val statusTextView: TextView = itemView.findViewById(R.id.textStatus)


        fun bind(leads: NewLeadsList) {
            nameTextView.text = leads.sName
            mobTextView.text = leads.leadStatus
//            courseTextView.text = leads.course_name.toString()
            statusTextView.text = leads.leadStatus


            itemView.setOnClickListener {
                onCategoryItemClickListener?.onCategoryItemClick(leads)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val itemView = LayoutInflater.from(context).inflate(R.layout.item_leads, parent, false)
        return CategoryViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(filteredList[position])
    }

    override fun getItemCount(): Int {
        return filteredList.size
    }

    fun filterByStatus(status: String?) {
        filteredList = if (status.isNullOrEmpty()) {
            ArrayList(categoryList)
        } else {
            ArrayList(categoryList.filter { it.leadStatus.equals(status, ignoreCase = true) })
        }
        notifyDataSetChanged()
    }
}