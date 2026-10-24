package com.liangleionline.shoubianshi

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.liangleionline.shoubianshi.data.Task
import com.liangleionline.shoubianshi.databinding.ActivityManageBinding
import kotlinx.coroutines.launch

/** 后台管理页：新增 / 编辑 / 删除全部事项。 */
class ManageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageBinding
    private val vm: MainViewModel by viewModels()
    private lateinit var adapter: ManageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = ManageAdapter(
            onEdit = ::showEditDialog,
            onDelete = ::confirmDelete
        )
        binding.manageRv.layoutManager = LinearLayoutManager(this)
        binding.manageRv.adapter = adapter

        binding.btnBack.setOnClickListener { finish() }
        binding.btnAdd.setOnClickListener { addTask() }
        binding.inputTask.setOnEditorActionListener { _, _, _ -> addTask(); true }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.allTasks.collect { list ->
                    adapter.submit(list)
                    binding.manageEmpty.visibility =
                        if (list.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun addTask() {
        val title = binding.inputTask.text?.toString()?.trim().orEmpty()
        if (title.isBlank()) return
        vm.addTask(title)
        binding.inputTask.text?.clear()
    }

    private fun showEditDialog(task: Task) {
        val input = EditText(this).apply {
            setText(task.title)
            setSelection(task.title.length)
            hint = getString(R.string.edit_dialog_hint)
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.edit_dialog_title)
            .setView(input)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_save) { _, _ ->
                val newTitle = input.text?.toString()?.trim().orEmpty()
                if (newTitle.isNotBlank()) {
                    vm.updateTask(task.copy(title = newTitle))
                }
            }
            .show()
    }

    private fun confirmDelete(task: Task) {
        AlertDialog.Builder(this)
            .setTitle(R.string.action_delete)
            .setMessage("确定删除“${task.title}”吗？")
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_delete) { _, _ -> vm.delete(task) }
            .show()
    }
}
