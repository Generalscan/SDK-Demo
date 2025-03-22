package com.generalscan.sdkapp.ui.activity.base

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import com.generalscan.sdkapp.R
import com.generalscan.sdkapp.support.inject.ViewInject
import com.generalscan.sdkapp.support.utils.MessageBox
import com.generalscan.sdkapp.support.utils.StringComparator
import com.generalscan.sdkapp.support.utils.Utils
import java.io.File
import java.util.*


class FileDialog : BaseActivity() {

    private var path: MutableList<String>? = null

    private var mList: ArrayList<HashMap<String, Any>>? = null

    private var inputManager: InputMethodManager? = null


    private var mParentPath: String = ""
    private var mCurrentPath = ROOT

    private var mSelectionMode = SelectionMode.MODE_CREATE

    private var mSelectionTarget = SelectionTarget.FILE

    private var mFormatFilter: Array<String>? = null

    //private boolean canSelectDir = false;

    private var mSelectedFile: File? = null
    private val mLastPositions = HashMap<String, Int>()
    private var mListAdapter: SimpleAdapter? = null

    @ViewInject(id = R.id.lblCurrentPath)
    private lateinit var mLblCurrentPath: TextView

    @ViewInject(id = R.id.lblNewFileOrFolderName)
    private lateinit var nLblNewFileOrFolderName: TextView


    @ViewInject(id = R.id.txtNewFileOrFolderName)
    private lateinit var mtxtNewFileOrFolderName: EditText

    @ViewInject(id = R.id.fdButtonSelect)
    private lateinit var selectButton: Button

    @ViewInject(id = R.id.fdLinearLayoutSelect)
    private lateinit var mLayoutSelect: LinearLayout

    @ViewInject(id = R.id.fdLinearLayoutCreate)
    private lateinit var mLayoutCreate: LinearLayout

    @ViewInject(id = android.R.id.list, itemClick = "onListItemClick")
    private lateinit var mListView: ListView

    private var fileSelected = false
    /**
     * Called when the activity is first created. Configura todos os parametros
     * de entrada e das VIEWS..
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED, intent)
        try {
            setContentView(R.layout.file_dialog_main)
            mSelectionMode = intent.getIntExtra(SELECTION_MODE, SelectionMode.MODE_CREATE)
            mSelectionTarget = intent.getIntExtra(SELECTION_TARGET, SelectionTarget.FILE)
            mFormatFilter = intent.getStringArrayExtra(FORMAT_FILTER)
            var title = intent.getStringExtra(TITLE)
            if(!title.isNullOrEmpty())
            {
                supportActionBar!!.title = title
            }
            inputManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

            selectButton.isEnabled = false
            selectButton.setOnClickListener {
                if (mSelectedFile != null) {
                    fileSelected = true
                    intent.putExtra(RESULT_PATH, mSelectedFile!!.path)
                    setResult(Activity.RESULT_OK, intent)
                    finish()
                }
            }


            val newButton = findViewById(R.id.fdButtonNew) as Button
            if (mSelectionMode == SelectionMode.MODE_OPEN) {
                newButton.setText(android.R.string.cancel)
            }
            newButton.setOnClickListener { v ->
                if (mSelectionMode == SelectionMode.MODE_OPEN) {
                    this@FileDialog.finish()
                } else {
                    setCreateVisible(v)
                    mtxtNewFileOrFolderName.setText("")
                    mtxtNewFileOrFolderName.requestFocus()
                }
            }

            if (mSelectionTarget == SelectionTarget.FILE && mSelectionMode == SelectionMode.MODE_OPEN) {
                newButton.visibility = View.GONE
                selectButton.visibility = View.GONE
            }


            //canSelectDir = getIntent().getBooleanExtra(CAN_SELECT_DIR, false);


            mLayoutCreate.visibility = View.GONE

            val cancelButton = findViewById(R.id.fdButtonCancel) as Button
            cancelButton.setOnClickListener { v -> setSelectVisible(v) }
            val createButton = findViewById(R.id.fdButtonCreate) as Button
            createButton.setOnClickListener { v ->
                if (mtxtNewFileOrFolderName.text.length > 0) {
                    if (mSelectionTarget == SelectionTarget.FILE) {
                        intent.putExtra(RESULT_PATH, mCurrentPath + "/" + mtxtNewFileOrFolderName.text)
                        fileSelected = true
                        setResult(Activity.RESULT_OK, intent)
                        finish()
                    } else {
                        val dir = File(mCurrentPath + "/" + mtxtNewFileOrFolderName.text)
                        dir.mkdir()
                        getDir(mCurrentPath)
                        setSelectVisible(v)
                    }
                }
            }

            var startPath: String? = intent.getStringExtra(START_PATH)
            startPath = if (startPath != null) startPath else ROOT
            if (mSelectionTarget == SelectionTarget.FOLDER) {
                val file = File(startPath)
                mSelectedFile = file
                selectButton.isEnabled = true
                nLblNewFileOrFolderName.setText(R.string.folder_name)
            }
            getDir(startPath)
        } catch (e: Exception) {
            //Utils.writeLog("Open File Dialog", e);
            MessageBox.showToastMessage(this, Utils.getErrorMessage(e))
        }

    }


    private fun getDir(dirPath: String) {

        val useAutoSelection = dirPath.length < mCurrentPath.length
        val position = mLastPositions[mParentPath]
        getDirImpl(dirPath)
        if (position != null && useAutoSelection) {
            mListView.setSelection(position)
        }

    }

    /**
     * Monta a estrutura de arquivos e diretorios filhos do diretorio fornecido.

     * @param dirPath Diretorio pai.
     */
    private fun getDirImpl(dirPath: String) {

        mCurrentPath = dirPath

        if (mSelectionTarget == SelectionTarget.FOLDER) {
            for (i in mFormatFilter!!.indices) {
                val formatLwr = mFormatFilter!![i].toLowerCase()
                if (formatLwr == mCurrentPath.toLowerCase()) {
                    selectButton.isEnabled = false
                    break
                }
            }
        }
        val item = ArrayList<String>()
        path = ArrayList<String>()
        mList = ArrayList<HashMap<String, Any>>()

        var f = File(mCurrentPath)
        var files: Array<File>? = f.listFiles()
        if (files == null) {
            mCurrentPath = ROOT
            f = File(mCurrentPath)
            files = f.listFiles()
        }
        mLblCurrentPath.text = getText(R.string.location).toString() + ": " + mCurrentPath

        if (mCurrentPath != ROOT) {

            item.add(ROOT)
            addItem(ROOT, R.drawable.ic_folder)
            path!!.add(ROOT)

            item.add("../")
            addItem("../", R.drawable.ic_folder)
            path!!.add(f.parent)
            mParentPath = f.parent

        }
        val comparator = StringComparator()
        val dirsMap = TreeMap<String, String>(comparator)
        val dirsPathMap = TreeMap<String, String>(comparator)
        val filesMap = TreeMap<String, String>(comparator)
        val filesPathMap = TreeMap<String, String>(comparator)
        if(files==null)
            return
        for (file in files) {
            if (file.isHidden)
                continue
            if (file.isDirectory) {
                val dirName = file.name
                dirsMap.put(dirName, dirName)
                dirsPathMap.put(dirName, file.path)
            } else {
                val fileName = file.name
                val fileNameLwr = fileName.toLowerCase()
                // se ha um filtro de formatos, utiliza-o
                if (mFormatFilter != null) {
                    var contains = false
                    for (i in mFormatFilter!!.indices) {
                        val formatLwr = mFormatFilter!![i].toLowerCase()
                        if (fileNameLwr.endsWith(formatLwr) ||
                                ((formatLwr == "*.*" || formatLwr == "*" || formatLwr == ".*") && mSelectionMode == SelectionMode.MODE_OPEN)
                                ){
                            contains = true
                            break
                        }
                    }
                    if (contains) {
                        filesMap.put(fileName, fileName)
                        filesPathMap.put(fileName, file.path)
                    }
                    // senao, adiciona todos os arquivos
                } else {
                    filesMap.put(fileName, fileName)
                    filesPathMap.put(fileName, file.path)
                }
            }
        }
        item.addAll(dirsMap.tailMap("").values)
        item.addAll(filesMap.tailMap("").values)
        path!!.addAll(dirsPathMap.tailMap("").values)
        path!!.addAll(filesPathMap.tailMap("").values)

        mListAdapter = SimpleAdapter(this, mList, R.layout.file_dialog_row, arrayOf(ITEM_KEY, ITEM_IMAGE), intArrayOf(R.id.fdrowtext, R.id.fdrowimage))

        for (dir in dirsMap.tailMap("").values) {
            addItem(dir, R.drawable.ic_folder)
        }
        if (mSelectionTarget == SelectionTarget.FILE) {
            for (file in filesMap.tailMap("").values) {
                addItem(file, R.drawable.ic_file)
            }
        }
        mListView.adapter = mListAdapter;
        mListAdapter!!.notifyDataSetChanged()


    }

    private fun addItem(fileName: String, imageId: Int) {
        val item = HashMap<String, Any>()
        item.put(ITEM_KEY, fileName)
        item.put(ITEM_IMAGE, imageId)
        mList!!.add(item)
    }


    /**
     * Quando clica no item da lista, deve-se: 1) Se for diretorio, abre seus
     * arquivos filhos; 2) Se puder escolher diretorio, define-o como sendo o
     * path escolhido. 3) Se for arquivo, define-o como path escolhido. 4) Ativa
     * botao de selecao.
     */
    fun onListItemClick(adapterView: AdapterView<*>, view: View, position: Int, id: Long) {

        val file = File(path!![position])

        setSelectVisible(view)

        if (file.isDirectory) {
            selectButton.isEnabled = false
            if (file.canRead()) {
                mLastPositions.put(mCurrentPath, position)
                getDir(path!![position])
                if (mSelectionTarget == SelectionTarget.FOLDER) {
                    mSelectedFile = file
                    view.isSelected = true
                    selectButton.isEnabled = true
                    for (i in mFormatFilter!!.indices) {
                        val formatLwr = mFormatFilter!![i].toLowerCase()
                        if (formatLwr == mCurrentPath.toLowerCase()) {
                            selectButton.isEnabled = false
                            break
                        }
                    }

                }
            } else {
                MessageBox.showWarningMessage(this@FileDialog, "[${file.name}]${getText(R.string.cant_read_folder)}")
                //AlertDialog.Builder(this).setTitle("[" + file.name + "] " + getText(R.string.cant_read_folder))
                //        .setPositiveButton(android.R.string.ok) { dialog, which -> }.show()
            }
        } else {
            mSelectedFile = file
            intent.putExtra(RESULT_PATH, mSelectedFile!!.path)
            fileSelected = true
            setResult(Activity.RESULT_OK, intent)
            finish()
            /*
            v.setSelected(true);
            selectButton.setEnabled(true);
            */
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            selectButton.isEnabled = false

            if (mLayoutCreate.visibility == View.VISIBLE) {
                mLayoutCreate.visibility = View.GONE
                mLayoutSelect.visibility = View.VISIBLE
            } else {
                if (mCurrentPath != ROOT) {
                    getDir(mParentPath)
                } else {
                    return super.onKeyDown(keyCode, event)
                }
            }

            return true
        } else {
            return super.onKeyDown(keyCode, event)
        }
    }

    /**
     * Define se o botao de CREATE e visivel.

     * @param v
     */
    private fun setCreateVisible(v: View) {
        mLayoutCreate.visibility = View.VISIBLE
        mLayoutSelect.visibility = View.GONE

        inputManager!!.hideSoftInputFromWindow(v.windowToken, 0)
        selectButton.isEnabled = false
    }

    /**
     * Define se o botao de SELECT e visivel.

     * @param v
     */
    private fun setSelectVisible(v: View) {
        mLayoutCreate.visibility = View.GONE
        mLayoutSelect.visibility = View.VISIBLE

        inputManager!!.hideSoftInputFromWindow(v.windowToken, 0)
        selectButton.isEnabled = false
    }


    object SelectionMode {
        val MODE_CREATE = 0
        val MODE_OPEN = 1
    }

    object SelectionTarget {
        val FILE = 0
        val FOLDER = 1
    }

    companion object {

        /**
         * Chave de um item da lista de paths.
         */
        private val ITEM_KEY = "key"

        /**
         * Imagem de um item da lista de paths (diretorio ou arquivo).
         */
        private val ITEM_IMAGE = "image"

        /**
         * Root Folder.
         */
        private val ROOT = "/"

        /**
         *
         * [Input Parameter] Initial path. DEFAULT: ROOT.
         */
        val START_PATH = "START_PATH"

        /**
         * [Input Parameter]: Format filter. DEFAULT: null
         *
         */
        val FORMAT_FILTER = "FORMAT_FILTER"

        /**
         * [Output Parameter]: Path chosen. DEFAULT: null.
         */
        val RESULT_PATH = "RESULT_PATH"


        /**
         * [Input Parameter]: Selection Mode (Open/Create). DEFAULT: Open.
         */
        val SELECTION_MODE = "SELECTION_MODE"

        /**
         * [Input Parameter]: Selection Target (Folder/File). DEFAULT: File.
         */
        val SELECTION_TARGET = "SELECTION_TARGET"


        /**
         * [Input Parameter]: Toolbar title. DEFAULT: "File Dialog".
         */
        val TITLE = "TITLE"

        /**
         * Parametro de entrada da Activity: se e permitido escolher diretorios.
         * Padrao: falso.
         */
        val CAN_SELECT_DIR = "CAN_SELECT_DIR"
    }
}
