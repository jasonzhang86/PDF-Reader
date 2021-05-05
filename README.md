# PDF Reader
PDF Reader is an Android application that allows a user to read and annotate a document on an Android tablet. <br>

The user can swipe to switch between pages, pinch to zoom in/out and drag to move the page around. <br>
The user can also undo/redo changes made on each page (such as annotations). The undo/redo stack is independent for each page. <br>


Developer's Journal:

0. IMPORTANT: Must build - clean the project before running the application, otherwise app generates a new BuildConfig file. (According to piazza @616 something is wrong with the grade config file, but I've re-downloaded the starter code and it seems that this problem comes with the starter code). To solve this, run Build - clean before running the app.
     
     UPDATE: According to the discussions under Piazza @726, this issue might be caused by having extra module. Howeverm I only have one module in Android Studio (I do have multiple in Intellji but I switched to Android Studio half way, not sure if this is the cause). If this error occurs again please run Build - clean.
     
1. Toolbar is on the top right corner (Touch, Draw, Highlight and Erase).

2. All testing are done using the shannon1948.pdf. I use 55 as the upper limit for the number of pages of the pdf file.

3. Eraser: It seems that the eraser doesn't work as well under high speed. Normal speed is fine; user is not required to purposely slow down their action, but a really fast swipe (like less than 0.1 second) might cause eraser to malfunction.

4. Also, the precision of the eraser can be off sometimes (depending on the shape of drawing) since it uses region of a path to detect collision. I've test it with many shapes and the only difficultly is erasing a vertical line vertically or a horizontal line horizontally (which can be done, but might take several tries). Please try multiple times if the first erase attempt does not work (and maybe try different spot to erase the same path)

5. Be very careful about drawing; sometimes undo/redo seems not working because the app undo/redo a tiny dot drew on the View (which wasn't intented by the user). To test undo/redo please make sure the lines drew are big enough (usually highlights are obvious enough but not for the pen). Also, please click on the undo/redo buttons with Touch mode on.

6. Undo/Redo stacks are independent for each page in the document. If the user draws something on page1 and switch to page2, undo/redo doesn't do anything (since there is nothing on page2). User has to go back to page1 to perform undo/redo (or draw something on page2).

7. Zoom/Pan: Zoom & Pan are only avaiable in Touch mode. One finger activates pan and two fingers (pinch) activates Zoom.

openjdk version "11.0.8" 2020-07-14 <br>
SDKs:&nbsp;&nbsp;Android API 30 Platform,&nbsp;&nbsp;Java SDK 11 <br>
macOS 10.14.6 (MacBook Pro 2019) <br>
pdf used: shannon1948.pdf <br>
Module used: Pixel C Tablet <br>
