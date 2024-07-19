package com.censorchi.views.adapter

import android.content.Context
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.censorchi.R
import com.censorchi.databinding.BlurLayoutBinding
import com.censorchi.views.activities.VideoEditActivity
import com.censorchi.views.model.blurFilterModel

class BlurFiltersAdapter(
    private val context: Context,
    private val list: ArrayList<blurFilterModel>,
    private var listener: ItemClick
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding: BlurLayoutBinding = BlurLayoutBinding.inflate(
            LayoutInflater.from(context), parent, false
        )
        return VH_BlurFilterAdapter(binding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val data = list[position]
        if (holder is VH_BlurFilterAdapter) {
            holder.binding.apply {
                tvBlurType.text = data.title
                ivBlurType.setImageResource(data.image)
                if (data.isSelected) {
                    if (position == 0) {
//                        if (VideoEditActivity.topBlur || VideoEditActivity.bottomBlur || VideoEditActivity.fullBlur) {
//                            cv.setCardBackgroundColor(
//                                ContextCompat.getColor(
//                                    context,
//                                    R.color.selected_btn_color
//                                )
//                            )
//                            tvBlurType.setTextColor(ContextCompat.getColor(context, R.color.white))
//
//                            if (position == list.size - 1) {
//                                ivBlurType.setImageResource(R.drawable.ic_full_blur_white)
//                            } else {
//                                ivBlurType.setColorFilter(
//                                    ContextCompat.getColor(context, R.color.white),
//                                    android.graphics.PorterDuff.Mode.SRC_IN
//                                )
//                            }
//                        }
//                        else {
//                            cv.setCardBackgroundColor(
//                                ContextCompat.getColor(
//                                    context,
//                                    R.color.selected_btn_color
//                                )
//                            )
//                            tvBlurType.setTextColor(ContextCompat.getColor(context, R.color.white))
//
//                            if (position == list.size - 1) {
//                                ivBlurType.setImageResource(R.drawable.ic_full_blur_white)
//                            } else {
//                                ivBlurType.setColorFilter(
//                                    ContextCompat.getColor(context, R.color.white),
//                                    android.graphics.PorterDuff.Mode.SRC_IN
//                                )
//                            }
//                        }
                    }
                    else {
                        cv.setCardBackgroundColor(
                            ContextCompat.getColor(
                                context,
                                R.color.selected_btn_color
                            )
                        )
                        tvBlurType.setTextColor(ContextCompat.getColor(context, R.color.white))

                        if (position == list.size - 1) {
                            ivBlurType.setImageResource(R.drawable.ic_full_blur_white)
                        } else {
                            ivBlurType.setColorFilter(
                                ContextCompat.getColor(context, R.color.white),
                                android.graphics.PorterDuff.Mode.SRC_IN
                            )
                        }
                    }
                }
                else {
//                    if (position == 0) {
//                        if (VideoEditActivity.topBlur || VideoEditActivity.bottomBlur || VideoEditActivity.fullBlur) {
//                            defaultView(position)
//
//                        }
//                        else {
//                            cv.setCardBackgroundColor(
//                                ContextCompat.getColor(
//                                    context,
//                                    R.color.selected_btn_color
//                                )
//                            )
//                            tvBlurType.setTextColor(ContextCompat.getColor(context, R.color.white))
//
//                            if (position == list.size - 1) {
//                                ivBlurType.setImageResource(R.drawable.ic_full_blur_white)
//                            } else {
//                                ivBlurType.setColorFilter(
//                                    ContextCompat.getColor(context, R.color.white),
//                                    android.graphics.PorterDuff.Mode.SRC_IN
//                                )
//                            }
//                        }
//                    }else{
//                        defaultView(position)
//                    }

                }
            }
        }

        holder.itemView.setOnClickListener {
            listener.onItemClick(position, data)
        }
    }

    fun BlurLayoutBinding.defaultView(position: Int) {
        cv.setCardBackgroundColor(
            ContextCompat.getColor(
                context,
                R.color.un_selected_button
            )
        )
        tvBlurType.setTextColor(ContextCompat.getColor(context, R.color.grey_color))
        if (position == list.size - 1) {
            ivBlurType.setImageResource(R.drawable.ic_full_blur)
        } else {
            ivBlurType.setColorFilter(
                ContextCompat.getColor(
                    context,
                    R.color.grey_color
                ), PorterDuff.Mode.SRC_IN
            )
        }
    }

    override fun getItemCount(): Int {
        return this.list.size
    }


    interface ItemClick {
        fun onItemClick(index: Int, item: blurFilterModel)
    }

    fun addItems(postItems: ArrayList<blurFilterModel>) {
        list.clear()
        list.addAll(postItems)
        notifyDataSetChanged()
    }


}