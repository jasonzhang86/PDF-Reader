package ca.uwaterloo.cs349.pdfreader;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.*;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.Stack;
import java.lang.Math;

import static ca.uwaterloo.cs349.pdfreader.MainActivity.*;

enum tool_type {
    touch,
    draw,
    highlight,
    erase,
}

class Undo {
    String undo_action;
    Path undo_path;

    public Undo(String s, Path p) {
        undo_action = s;
        undo_path = p;
    }
}

class Redo {
    String redo_action;
    Path redo_path;

    public Redo(String s, Path p) {
        redo_action = s;
        redo_path = p;
    }
}

@SuppressLint("AppCompatCustomView")
public class PDFimage extends ImageView {

    final String LOGNAME = "pdf_image";

    // drawing path
    Path path = null;
    //Path erasePath = null;
    static ArrayList<Path> draws = drawAnnotations.get(pageNumber);
    static ArrayList<Path> highlights = highlightAnnotations.get(pageNumber);
    static Stack<Undo> undoStack = allUndo.get(pageNumber);
    static Stack<Redo> redoStack = allRedo.get(pageNumber);

    // image to display
    Bitmap bitmap;
    Paint paint = new Paint(Color.BLUE);

    // constructor
    public PDFimage(Context context) {
        super(context);
    }

    // Touch Event
    float startX = -1;
    float startY = -1;
    float curX = -1;
    float curY = -1;
    float x1 = -1;
    float y1 = -1;
    float x2 = -1;
    float y2 = -1;
    float distance = -1;
    boolean dis_ready = false;
    float ScaleFactor = 1.0f;
    boolean need_inverse = false;
    Matrix matrix = new Matrix();

    // capture touch events (down/move/up) to create a path
    // and use that to create a stroke that we can draw
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                Log.d(LOGNAME, "Action down");
                path = new Path();
                path.moveTo(event.getX(), event.getY());
                if (cur_tool == tool_type.touch) {
                    // single touch for pan
                    if (event.getPointerCount() == 1) {
                        float x = event.getX();
                        float y = event.getY();
                        startX = x;
                        startY = y;
                        curX = x;
                        curY = y;
                        dis_ready = false;
                    } else {
                        // First Point
                        int p1_id = event.getPointerId(0);
                        int p1_index = event.findPointerIndex(p1_id);
                        x1 = event.getX(p1_index);
                        y1 = event.getY(p1_index);

                        // Second Point
                        int p2_id = event.getPointerId(1);
                        int p2_index = event.findPointerIndex(p2_id);
                        x2 = event.getX(p2_index);
                        y2 = event.getY(p2_index);

                        distance = (float) Math.sqrt(Math.pow(x1-x2, 2) + Math.pow(y1-y2, 2));
                        ScaleFactor = 1.0f;
                    }
                } else if (cur_tool == tool_type.draw) {
                    draws.add(path);
                } else if (cur_tool == tool_type.highlight) {
                    highlights.add(path);
                } else if (cur_tool == tool_type.erase) {
                    //erasePath = path;
                    int x = (int) event.getX();
                    int y = (int) event.getY();

                    ArrayList<Path> remove1 = new ArrayList<>();
                    for (Path p1: draws) {
                        RectF rectF = new RectF();
                        p1.computeBounds(rectF, true);
                        Region r1 = new Region();
                        r1.setPath(p1, new Region((int) rectF.left, (int) rectF.top, (int) rectF.right, (int) rectF.bottom));
                        if (r1.contains(x, y)) {
                            remove1.add(p1);
                        }
                    }
                    draws.removeAll(remove1);

                    ArrayList<Path> remove2 = new ArrayList<>();
                    for (Path p2: highlights) {
                        RectF rectF = new RectF();
                        p2.computeBounds(rectF, true);
                        Region r2 = new Region();
                        r2.setPath(p2, new Region((int) rectF.left, (int) rectF.top, (int) rectF.right, (int) rectF.bottom));
                        if (r2.contains(x, y)) {
                            remove2.add(p2);
                        }
                    }
                    highlights.removeAll(remove2);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                //Log.d(LOGNAME, "Action move");
                path.lineTo(event.getX(), event.getY());

                if (cur_tool == tool_type.touch) {
                    // single touch for pan
                    if (event.getPointerCount() == 1) {
                        float x = event.getX();
                        float y = event.getY();
                        curX = x;
                        curY = y;
                        float dX = curX - startX;
                        float dY = curY - startY;
                        matrix.setTranslate(dX, dY);
                        need_inverse = true;
                    } else {
                        // First Point
                        int p1_id = event.getPointerId(0);
                        int p1_index = event.findPointerIndex(p1_id);
                        x1 = event.getX(p1_index);
                        y1 = event.getY(p1_index);

                        // Second Point
                        int p2_id = event.getPointerId(1);
                        int p2_index = event.findPointerIndex(p2_id);
                        x2 = event.getX(p2_index);
                        y2 = event.getY(p2_index);

                        float cur_distance = (float) Math.sqrt(Math.pow(x1-x2, 2) + Math.pow(y1-y2, 2));
                        if (dis_ready == false) {
                            distance = cur_distance;
                            dis_ready = true;
                        }
                        ScaleFactor = cur_distance / distance;
                        ScaleFactor = Math.max(0.1f, Math.min(ScaleFactor, 8.0f));
                        matrix.setScale(ScaleFactor, ScaleFactor, (x1+x2)/2, (y1+y2)/2);
                        need_inverse = false;
                    }

                } else if (cur_tool == tool_type.erase) { /* eraser */
                    int x = (int) event.getX();
                    int y = (int) event.getY();

                    ArrayList<Path> remove1 = new ArrayList<>();
                    for (Path p1: draws) {
                        RectF rectF = new RectF();
                        p1.computeBounds(rectF, true);
                        Region r1 = new Region();
                        r1.setPath(p1, new Region((int) rectF.left, (int) rectF.top, (int) rectF.right, (int) rectF.bottom));
                        if (r1.contains(x, y)) {
                            remove1.add(p1);
                            Undo undo = new Undo("draw", p1);
                            undoStack.push(undo);
                        }
                    }
                    draws.removeAll(remove1);

                    ArrayList<Path> remove2 = new ArrayList<>();
                    for (Path p2: highlights) {
                        RectF rectF = new RectF();
                        p2.computeBounds(rectF, true);
                        Region r2 = new Region();
                        r2.setPath(p2, new Region((int) rectF.left, (int) rectF.top, (int) rectF.right, (int) rectF.bottom));
                        if (r2.contains(x, y)) {
                            remove2.add(p2);
                            Undo undo = new Undo("highlight", p2);
                            undoStack.push(undo);
                        }
                    }
                    highlights.removeAll(remove2);
                }
                break;

            case MotionEvent.ACTION_UP:
                //Log.d(LOGNAME, "Action up");
                String reverse_action = "";
                if (cur_tool == tool_type.touch) {

                } else if (cur_tool == tool_type.draw) {
                    reverse_action = "erase draw";
                    Undo undo = new Undo(reverse_action, path);
                    undoStack.push(undo);
                } else if (cur_tool == tool_type.highlight) {
                    reverse_action = "erase highlight";
                    Undo undo = new Undo(reverse_action, path);
                    undoStack.push(undo);
                }
                break;
        }
        return true;
    }

    // set image as background
    public void setImage(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    // set brush characteristics
    // e.g. color, thickness, alpha
    public void setBrush(Paint paint) {
        this.paint = paint;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // draw background
        if (bitmap != null) {
            this.setImageBitmap(bitmap);
        }

        if (need_inverse) {
            Matrix inverse = new Matrix();
            matrix.invert(inverse);
            canvas.concat(inverse);
        } else {
            canvas.concat(matrix);
        }

        for (Path path1: draws) {
            paint.setColor(Color.BLUE);
            paint.setStrokeWidth(8);
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawPath(path1, paint);
        }

        for (Path path2: highlights) {
            paint.setColor(Color.YELLOW);
            paint.setStrokeWidth(20);
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawPath(path2, paint);
        }

        super.onDraw(canvas);
    }

}
