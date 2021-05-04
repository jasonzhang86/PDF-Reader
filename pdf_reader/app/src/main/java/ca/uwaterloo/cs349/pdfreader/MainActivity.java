package ca.uwaterloo.cs349.pdfreader;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Path;
import android.graphics.Region;
import android.graphics.pdf.PdfRenderer;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

import static ca.uwaterloo.cs349.pdfreader.PDFimage.*;

// PDF sample code from
// https://medium.com/@chahat.jain0/rendering-a-pdf-document-in-android-activity-fragment-using-pdfrenderer-442462cb8f9a
// Issues about cache etc. are not obvious from documentation, so read this carefully before making changes
// to the PDF display code.

public class MainActivity extends AppCompatActivity {

    final String LOGNAME = "pdf_viewer";
    final String FILENAME = "shannon1948.pdf";
    final int FILERESID = R.raw.shannon1948;

    static tool_type cur_tool = tool_type.touch;
    static int pageNumber = 0;
    static ArrayList<ArrayList<Path>> drawAnnotations = new ArrayList<>();
    static ArrayList<ArrayList<Path>> highlightAnnotations = new ArrayList<>();
    static ArrayList<Stack<Undo>> allUndo = new ArrayList<>();
    static ArrayList<Stack<Redo>> allRedo = new ArrayList<>();

    // manage the pages of the PDF, see below
    PdfRenderer pdfRenderer;
    private ParcelFileDescriptor parcelFileDescriptor;
    private PdfRenderer.Page currentPage;

    // custom ImageView class that captures strokes and draws them over the image
    PDFimage pageImage;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Button prevPage = (Button) findViewById(R.id.previous);
        Button nextPage = (Button) findViewById(R.id.next);
        Button undoAct = (Button) findViewById(R.id.undo);
        Button redoAct = (Button) findViewById(R.id.redo);

        undoAct.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!undoStack.empty()) {
                    Undo undo = undoStack.pop();
                    Path undo_path = undo.undo_path;
                    switch (undo.undo_action) {
                        case "draw":
                            draws.add(undo_path);
                            Redo redo1 = new Redo("erase draw", undo_path);
                            redoStack.push(redo1);
                            break;
                        case "highlight":
                            highlights.add(undo_path);
                            Redo redo2 = new Redo("erase highlight", undo_path);
                            redoStack.push(redo2);
                            break;
                        case "erase draw":
                            draws.remove(undo_path);
                            Redo redo3 = new Redo("draw", undo_path);
                            redoStack.push(redo3);
                            break;
                        case "erase highlight":
                            highlights.remove(undo_path);
                            Redo redo4 = new Redo("highlight", undo_path);
                            redoStack.push(redo4);
                            break;
                    }
                }
            }
        });

        redoAct.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!redoStack.empty()) {
                    Redo redo = redoStack.pop();
                    Path redo_path = redo.redo_path;
                    switch (redo.redo_action) {
                        case "draw":
                            draws.add(redo_path);
                            Undo undo1 = new Undo("erase draw", redo_path);
                            undoStack.push(undo1);
                            break;
                        case "highlight":
                            highlights.add(redo_path);
                            Undo undo2 = new Undo("erase highlight", redo_path);
                            undoStack.push(undo2);
                            break;
                        case "erase draw":
                            draws.remove(redo_path);
                            Undo undo3 = new Undo("draw", redo_path);
                            undoStack.push(undo3);
                            break;
                        case "erase highlight":
                            highlights.remove(redo_path);
                            Undo undo4 = new Undo("highlight", redo_path);
                            undoStack.push(undo4);
                            break;
                    }
                }
            }
        });

        prevPage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (pageNumber > 0) {
                    pageNumber--;
                    showPage(pageNumber);
                    draws = drawAnnotations.get(pageNumber);
                    highlights = highlightAnnotations.get(pageNumber);
                    undoStack = allUndo.get(pageNumber);
                    redoStack = allRedo.get(pageNumber);
                    TextView tv = (TextView)findViewById(R.id.pageNum);
                    int cur_page = pageNumber + 1;
                    tv.setText("Page " + cur_page + "/55");
                }
            }
        });

        nextPage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (pageNumber < 54) {
                    pageNumber++;
                    showPage(pageNumber);
                    draws = drawAnnotations.get(pageNumber);
                    highlights = highlightAnnotations.get(pageNumber);
                    undoStack = allUndo.get(pageNumber);
                    redoStack = allRedo.get(pageNumber);
                    TextView tv = (TextView)findViewById(R.id.pageNum);
                    int cur_page = pageNumber + 1;
                    tv.setText("Page " + cur_page + "/55");
                }
            }
        });

        for (int i = 0; i < 55; i++) {
            drawAnnotations.add(new ArrayList<Path>());
            highlightAnnotations.add(new ArrayList<Path>());
            allUndo.add(new Stack<Undo>());
            allRedo.add(new Stack<Redo>());
        }

        LinearLayout layout = findViewById(R.id.pdfLayout);
        pageImage = new PDFimage(this);
        layout.addView(pageImage);
        layout.setEnabled(true);
        pageImage.setPadding(0,50,0,0);
        pageImage.setMinimumWidth(1000);
        pageImage.setMinimumHeight(2000);

        // open page 0 of the PDF
        // it will be displayed as an image in the pageImage (above)
        try {
            openRenderer(this);
            showPage(0);
        } catch (IOException exception) {
            Log.d(LOGNAME, "Error opening PDF");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_touch) {
            cur_tool = tool_type.touch;
            return true;
        } else if (id == R.id.action_draw) {
            cur_tool = tool_type.draw;
            return true;
        } else if (id == R.id.action_highlight) {
            cur_tool = tool_type.highlight;
            return true;
        } else if (id == R.id.action_erase) {
            cur_tool = tool_type.erase;
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            closeRenderer();
        } catch (IOException ex) {
            Log.d(LOGNAME, "Unable to close PDF renderer");
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void openRenderer(Context context) throws IOException {
        // In this sample, we read a PDF from the assets directory.
        File file = new File(context.getCacheDir(), FILENAME);
        if (!file.exists()) {
            // pdfRenderer cannot handle the resource directly,
            // so extract it into the local cache directory.
            InputStream asset = this.getResources().openRawResource(FILERESID);
            FileOutputStream output = new FileOutputStream(file);
            final byte[] buffer = new byte[1024];
            int size;
            while ((size = asset.read(buffer)) != -1) {
                output.write(buffer, 0, size);
            }
            asset.close();
            output.close();
        }
        parcelFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);

        // capture PDF data
        // all this just to get a handle to the actual PDF representation
        if (parcelFileDescriptor != null) {
            pdfRenderer = new PdfRenderer(parcelFileDescriptor);
        }
    }

    // do this before you quit!
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void closeRenderer() throws IOException {
        if (null != currentPage) {
            currentPage.close();
        }
        pdfRenderer.close();
        parcelFileDescriptor.close();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void showPage(int index) {
        if (pdfRenderer.getPageCount() <= index) {
            return;
        }
        // Close the current page before opening another one.
        if (null != currentPage) {
            currentPage.close();
        }
        // Use `openPage` to open a specific page in PDF.
        currentPage = pdfRenderer.openPage(index);
        // Important: the destination bitmap must be ARGB (not RGB).
        Bitmap bitmap = Bitmap.createBitmap(currentPage.getWidth(), currentPage.getHeight(), Bitmap.Config.ARGB_8888);

        // Here, we render the page onto the Bitmap.
        // To render a portion of the page, use the second and third parameter. Pass nulls to get the default result.
        // Pass either RENDER_MODE_FOR_DISPLAY or RENDER_MODE_FOR_PRINT for the last parameter.
        currentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

        // Display the page
        pageImage.setImage(bitmap);
    }
}
