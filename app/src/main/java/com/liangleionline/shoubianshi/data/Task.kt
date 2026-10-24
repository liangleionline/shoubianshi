package com.liangleionline.shoubianshi.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 待办事项。
 * isPinned = true 表示该事项已拖入「接下来待办」区（上区）；
 * isPinned = false 表示该事项仍在「全部事项」素材库（下区）。
 */
@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
