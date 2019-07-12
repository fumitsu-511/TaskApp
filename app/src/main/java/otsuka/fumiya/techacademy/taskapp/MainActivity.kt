package otsuka.fumiya.techacademy.taskapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.icu.text.CaseMap
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity;
import io.realm.Realm
import io.realm.RealmChangeListener
import io.realm.Sort
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

const val EXTRA_TASK = "otsuka.fumiya.techacademy.taskapp.TASK"

class MainActivity : AppCompatActivity() {

    private lateinit var mRealm: Realm
    private val mRealmListener = object : RealmChangeListener<Realm>{
        override fun onChange(element: Realm) {
            reloadListView()
        }
    }

    private val category_searchOnclickListener = View.OnClickListener {
        categorySearch()
    }

    private lateinit var mTaskAdapter :TaskAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fab.setOnClickListener { _ ->
            val intent = Intent(this@MainActivity, InputActivity::class.java)
            startActivity(intent)
        }

        search_button.setOnClickListener(category_searchOnclickListener)

        //Realmの設定
        mRealm = Realm.getDefaultInstance()
        mRealm.addChangeListener(mRealmListener)

        //ListViewの設定
        mTaskAdapter = TaskAdapter(this@MainActivity)

        //ListViewをタップしたときの処理
        listView1.setOnItemClickListener{ parent, _, position, _->
            //入力・編集する画面に遷移させる
            val task = parent.adapter.getItem(position) as Task
            val intent = Intent(this@MainActivity, InputActivity::class.java)
            intent.putExtra(EXTRA_TASK, task.id)
            startActivity(intent)
        }

        //ListViewを長押ししたときの処理
        listView1.setOnItemLongClickListener { parent, _, position, _ ->
            // タスクを削除する
            val task = parent.adapter.getItem(position) as Task

            // ダイアログを表示する
            val builder = AlertDialog.Builder(this@MainActivity)

            builder.setTitle("削除")
            builder.setMessage(task.title + "を削除しますか")

            builder.setPositiveButton("OK"){_, _ ->
                val results = mRealm.where(Task::class.java).equalTo("id", task.id).findAll()

                mRealm.beginTransaction()
                results.deleteAllFromRealm()
                mRealm.commitTransaction()

                val resultIntent = Intent(applicationContext, TaskAlarmReceiver::class.java)
                val resultPendingIntent = PendingIntent.getBroadcast(
                    this@MainActivity,
                    task.id,
                    resultIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )

                val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                alarmManager.cancel(resultPendingIntent)

                reloadListView()
            }

            builder.setNegativeButton("CANCEL", null)

            val dialog = builder.create()
            dialog.show()

            true
        }

        reloadListView()
    }


    private fun categorySearch(){
        var sCategory :String? = searchText.text.toString()

        if (sCategory == null || sCategory=="") {

            reloadListView()

        }else {
            searchListView(sCategory)
        }

    }

    private fun reloadListView(){
        //Realmデータベースから、、「全てのデータを取得して新しい日時順に並べた結果」を取得
        val taskRealmResults = mRealm.where(Task::class.java).findAll().sort("date",Sort.DESCENDING)

        //上記の結果を、TaskListとしてセットする
        mTaskAdapter.taskList = mRealm.copyFromRealm(taskRealmResults)

        listView1.adapter = mTaskAdapter
        mTaskAdapter.notifyDataSetChanged()
    }

    private fun searchListView(category: String){
        //Realmデータベースから、、「全てのデータを取得して新しい日時順に並べた結果」を取得
        val taskRealmResults = mRealm.where(Task::class.java).contains("category",category).findAll().sort("date",Sort.DESCENDING)

        if (taskRealmResults == null || taskRealmResults.isEmpty()){
            val title = "検索結果が見つかりません。 "
            val message = "入力値を確認の上、もう一度検索してください。"
            showAlertDialog(title,message)

        }else{

            //上記の結果を、TaskListとしてセットする
            mTaskAdapter.taskList = mRealm.copyFromRealm(taskRealmResults)

            listView1.adapter = mTaskAdapter
            mTaskAdapter.notifyDataSetChanged()

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mRealm.close()
    }

    private fun showAlertDialog(title: String, message : String){

        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle(title)
        alertDialogBuilder.setMessage(message)

        alertDialogBuilder.setPositiveButton("OK"){
            _, _ -> return@setPositiveButton
        }

        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }

}

