
import org.opencv.core.Core;
import org.opencv.core.Rect;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Scalar;
//import org.opencv.core.Size;

import org.opencv.highgui.HighGui;
import org.opencv.highgui.ImageWindow;
//import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.io.File;
import java.io.PrintWriter;
import java.lang.Math;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;

public class App{
    static{ System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }

    static boolean Process = true;
    static String sol = "";
    static StringBuilder sb = new StringBuilder();

    static File csvFile = new File("C:\\VSCodeProjects\\puzzles\\test.csv");

    static List<Rect> squares = new ArrayList<Rect>();
    static int[][] sym_row_dist = {{0,0},{0,0},{0,0},{0,0},{0,0},{0,0},{0,0},{0,0},{0,0},{0,0},{0,0},{0,0},{0,0},{0,0},{0,0},{0,0},{0,0},{0,0},{0,0},{0,0}};
    static int[][] sym_col_dist = {{0,0},{0,0},{0,0},{0,0},{0,0},{0,0},{0,0},{0,0},{0,0},{0,0},{0,0},{0,0},{0,0},{0,0},{0,0},{0,0},{0,0},{0,0},{0,0},{0,0}};
    static int n_dist[][] = {{0,0,0,0,0},{0,0,0,0,0},{0,0,0,0,0},{0,0,0,0,0},{0,0,0,0,0},
                            {0,0,0,0,0},{0,0,0,0,0},{0,0,0,0,0},{0,0,0,0,0},{0,0,0,0,0},
                            {0,0,0,0,0},{0,0,0,0,0},{0,0,0,0,0},{0,0,0,0,0},{0,0,0,0,0},
                            {0,0,0,0,0},{0,0,0,0,0},{0,0,0,0,0},{0,0,0,0,0},{0,0,0,0,0},
                            {0,0,0,0,0},{0,0,0,0,0},{0,0,0,0,0},{0,0,0,0,0},{0,0,0,0,0}};

    static ArrayList<ArrayList<Double>> sym_row_norm = new ArrayList<ArrayList<Double>>();
    static ArrayList<ArrayList<Double>> sym_col_norm = new ArrayList<ArrayList<Double>>();
    static ArrayList<ArrayList<Double>> num_norm = new ArrayList<ArrayList<Double>>();

    static String row_metric = "";
    static String col_metric = "";
    static String num_metric = "";

    static Integer row_metric_1 = 0;
    static Integer col_metric_1 = 0;
    static Integer num_metric_1 = 0;

    static List<Integer> weighted = new ArrayList<Integer>();
    static List<Integer> num_weighted = new ArrayList<Integer>();
    public static void main(String[] args) throws Exception {
        //System.out.println(System.getProperty("java.library.path"));
        
        String filePath = "C:/VSCodeProjects/Images/PXL_20211010_191704981.mp4";
        //String filePath = "C:/Python/HoughLines/images/PXL_20211009_171953904.mp4";

        int fontface = Imgproc.FONT_HERSHEY_SIMPLEX;

        
        Scalar blue = new Scalar(255, 0, 0);
        Scalar green = new Scalar(0, 255, 0);
        Scalar red = new Scalar(0,0,255);
        
        VideoCapture capture = new VideoCapture(filePath);
        //capture.open(filePath);
        //capture.isOpened();
        Mat frame = new Mat();
        Mat gray = new Mat();
        Mat thresh = new Mat();
        Mat hierarchy = new Mat();
        //Mat resized = new Mat();

        MatOfPoint2f approxCurve = new MatOfPoint2f();

        List<Point> points= new ArrayList<Point>();
        //List<MatOfPoint> pts = new ArrayList<MatOfPoint>();

        HighGui.namedWindow( "frame", ImageWindow.WINDOW_AUTOSIZE);
        HighGui.resizeWindow("frame", 600, 800);

        int cx = 1920/2;
        int cy = 1080/2;
        int width = 50;
        int height = 50;

        int dyn_xmin = cx - 5 * width;
        int dyn_xmax = cx + 5 * width;
        int dyn_ymin = cy - 5 * height;
        int dyn_ymax = cy + 5 * height;

        while(true){
            if (capture.read(frame)){
                //System.out.println("Captured Frame Width " + frame.width() + " Height " + frame.height());
                //HighGui.imshow("frame", frame);
                //HighGui.waitKey(0);
                //Imgcodecs.imwrite("camera.jpg", frame);
                //if(HighGui.waitKey(30) >= 0) break;
                //break;
                //Float scale_percent = 921.0f/frame.height();
                //int height = Math.round(frame.height() * scale_percent);
                //int width = Math.round(frame.width() * scale_percent);
                //System.out.println(scale_percent + " " + height + " " + width);
                
                //Size scaleSize = new Size(height,width);
                //Imgproc.resize(frame, resized, scaleSize, 0, 0, Imgproc.INTER_AREA);
            }

            if (frame.empty()){
                System.out.println("empty frame");
                HighGui.destroyAllWindows();
                capture.release();
                System.exit(0);
                break;  // reach to the end of the video file
            }

            Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);
            Imgproc.adaptiveThreshold(gray, thresh,255,Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 15, 4);
            Core.bitwise_not(thresh,thresh);

            List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
            Imgproc.findContours(thresh.clone(), contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
            //Imgproc.drawContours(frame, contours, -1, blue);

            List<Rect> boxes = new ArrayList<Rect>();

            int sumx = 0, sumy = 0, sumw = 0, sumh = 0;

            for (MatOfPoint cnt : contours){

                MatOfPoint2f curve = new MatOfPoint2f(cnt.toArray());
                Imgproc.approxPolyDP(curve, approxCurve, 0.1 * Imgproc.arcLength(curve, true), true);
                //int numberVertices = (int) approxCurve.total();

                if ((int) approxCurve.total() == 4 && Imgproc.arcLength(curve,true) > 50 && Imgproc.contourArea(cnt) > 500){

                    Rect rect = Imgproc.boundingRect(cnt);

                    if (rect.x > dyn_xmin && rect.x < dyn_xmax && rect.y > dyn_ymin && rect.y < dyn_ymax){

                        boxes.add(rect);
                        sumx = sumx + rect.x;
                        sumy = sumy + rect.y;
                        sumw = sumw + rect.width;
                        sumh = sumh + rect.height;

                        Imgproc.rectangle(frame, new Point(rect.x,rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(255, 0, 0), 4);
                    }
                }
            }

            if (boxes.size() > 10){

                cx = (sumx + sumw) / boxes.size() - sumw / (2 * boxes.size());
                cy = (sumy + sumh) / boxes.size() - sumh / (2 * boxes.size());
                width = sumw / boxes.size();
                height = sumh / boxes.size();
                dyn_xmin = cx - 5 * width;
                dyn_xmax = cx + 5 * width;
                dyn_ymin = cy - 5 * height;
                dyn_ymax = cy + 5 * height;
            }

           
            

            //List<Object> symbols = new ArrayList<Object>();
            ArrayList<ArrayList<Object>> symbols = new ArrayList<ArrayList<Object>>();
            
            for (MatOfPoint cnt : contours){

                MatOfPoint2f curve = new MatOfPoint2f(cnt.toArray());
                Imgproc.approxPolyDP(curve, approxCurve, 0.1 * Imgproc.arcLength(curve, true), true);

                if ((int) approxCurve.total() == 4 && Imgproc.arcLength(curve,true) > 25 && Imgproc.arcLength(curve,true) < 500 && Imgproc.contourArea(cnt) > 25 && Imgproc.contourArea(cnt) < 500){
                    
                    Rect rect = Imgproc.boundingRect(cnt);

                    if (rect.x > dyn_xmin && rect.x < dyn_xmax && rect.y > dyn_ymin && rect.y < dyn_ymax){

                    //System.out.println(Arrays.toString(cnt.toArray()));
                    //System.out.println(Arrays.toString(approxCurve.toArray()));
                    points = cnt.toList();

                    points.sort(Comparator.comparing(point -> point.x));
                    Point leftmost = points.get(0);
                    Point rightmost = points.get(points.size() - 1);

                    points.sort(Comparator.comparing(point -> point.y));
                    Point topmost = points.get(0);
                    Point bottomost = points.get(points.size() - 1);

                    int t = 4;

                    if ((Math.abs(rightmost.x-topmost.x) + Math.abs(rightmost.x-bottomost.x)) < t && Math.abs(leftmost.x-topmost.x) > t ) {
                        //System.out.println("LEFT");
                        symbols.add(new ArrayList<>(Arrays.asList(rect,"L")));
                        Imgproc.putText(frame,"L",topmost,fontface,1, blue,2);
                    } else if ((Math.abs(leftmost.x-topmost.x) + Math.abs(leftmost.x-bottomost.x)) < t && Math.abs(leftmost.x-rightmost.x) > t) {
                        //System.out.println("RIGHT");
                        symbols.add(new ArrayList<>(Arrays.asList(rect,"R")));
                        Imgproc.putText(frame,"R",topmost,fontface,1,blue,2);
                    } else if ((Math.abs(topmost.y-rightmost.y) + Math.abs(topmost.y-leftmost.y)) < t && Math.abs(topmost.y - bottomost.y) > t) {
                        //System.out.println("DOWN");
                        symbols.add(new ArrayList<>(Arrays.asList(rect,"D")));
                        Imgproc.putText(frame,"D",topmost,fontface,1,blue,2);
                    } else if ((Math.abs(bottomost.y-rightmost.y) + Math.abs(bottomost.y-leftmost.y)) < t && Math.abs(topmost.y - bottomost.y) > t) {
                        //System.out.println("UP");
                        symbols.add(new ArrayList<>(Arrays.asList(rect,"U")));
                        Imgproc.putText(frame,"U",topmost,fontface,1,blue,2);
                    }

                    Imgproc.rectangle(frame, new Point(rect.x,rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(255, 0, 0), 4);

                    }
                }
            }

            ArrayList<ArrayList<Object>> numbers = new ArrayList<ArrayList<Object>>();

            for (Rect box : boxes){

                contours.clear();
                Imgproc.findContours(thresh.submat(box), contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_NONE, new Point(box.x,box.y));

                List<MatOfPoint> cnt_approx = new ArrayList<MatOfPoint>();

                for (MatOfPoint cnt : contours){
                    MatOfPoint2f curve = new MatOfPoint2f(cnt.toArray());

                    if (Imgproc.arcLength(curve,true) > 50 && Imgproc.arcLength(curve,true) < 300 && Imgproc.contourArea(cnt) > 50 && Imgproc.contourArea(curve) < 300){
                        
                        Imgproc.approxPolyDP(curve, approxCurve, 0.1 * Imgproc.arcLength(curve, true), true);

                        MatOfPoint mPoints = new MatOfPoint();
                        mPoints.fromList(approxCurve.toList());
                        cnt_approx.clear();
                        cnt_approx.add(mPoints);

                        Imgproc.drawContours(frame,cnt_approx,-1, blue,2);

                        points = cnt.toList();

                        points.sort(Comparator.comparing(point -> point.x));
                        Point leftmost = points.get(0);
                        Point rightmost = points.get(points.size() - 1);

                        points.sort(Comparator.comparing(point -> point.y));
                        Point topmost = points.get(0);
                        Point bottomost = points.get(points.size() - 1);


                        if (approxCurve.size(0) == 2){
                            //System.out.println("3");
                            Imgproc.putText(frame,"3",topmost,fontface,1, green,2);
                            Rect rect = Imgproc.boundingRect(cnt);
                            numbers.add(new ArrayList<>(Arrays.asList(rect,"3")));
                        } else if (approxCurve.size(0) > 2 && (Math.abs(leftmost.y-bottomost.y) < 4 || Math.abs(rightmost.y-bottomost.y) < 4)) {
                            //System.out.println("2");
                            Imgproc.putText(frame,"2",topmost,fontface,1, green,2);
                            Rect rect = Imgproc.boundingRect(cnt);
                            numbers.add(new ArrayList<>(Arrays.asList(rect,"2")));
                        } else if(approxCurve.size(0) > 2 && Math.abs(leftmost.y-bottomost.y) > 4){
                            //System.out.println("4");
                            Imgproc.putText(frame,"4",topmost,fontface,1, green,2);
                            Rect rect = Imgproc.boundingRect(cnt);
                            numbers.add(new ArrayList<>(Arrays.asList(rect,"4")));
                        }
                    }
                }
            }
            
            //System.out.println(numbers);
            //System.out.println(boxes.size());
            //System.out.println(symbols.size());

            if (boxes.size() == 25){
                Imgproc.rectangle(frame, new Point(dyn_xmin, dyn_ymin), new Point(dyn_xmax, dyn_ymax), green, 4);
            } else {
                Imgproc.rectangle(frame, new Point(dyn_xmin, dyn_ymin), new Point(dyn_xmax, dyn_ymax), red, 4);
            }

            Imgproc.putText(frame,Integer.toString(boxes.size()),new Point(dyn_xmin - 40, dyn_ymin + 20),fontface,0.50,red,1);

            Imgproc.circle( frame,new Point(dyn_xmin, dyn_ymin),10,new Scalar( 0, 0, 255 ),-1,1,0 );

            Imgproc.putText(frame,row_metric,new Point(dyn_xmin, dyn_ymax + 20),fontface,0.50,red,1);
            Imgproc.putText(frame,col_metric,new Point(dyn_xmin, dyn_ymax + 40),fontface,0.50,red,1);
            Imgproc.putText(frame,num_metric,new Point(dyn_xmin, dyn_ymax + 60),fontface,0.50,red,1);

            Imgproc.putText(frame,Integer.toString(row_metric_1),new Point(dyn_xmax, dyn_ymin + 20),fontface,0.50,red,1);
            Imgproc.putText(frame,Integer.toString(col_metric_1),new Point(dyn_xmax, dyn_ymin + 40),fontface,0.50,red,1);
            Imgproc.putText(frame,Integer.toString(num_metric_1),new Point(dyn_xmax, dyn_ymin + 60),fontface,0.50,red,1);

            if (Process == false) {
                int i = 0;
                for (Rect square : squares){
                    Imgproc.putText(frame,sol.substring(i,i+1),new Point(square.x + square.width/2, square.y + square.height/2),fontface,1,green,2);
                    i += 1;
                }
                Imgproc.putText(frame,sol,new Point(dyn_xmin, dyn_ymax),fontface,0.75,red,1);
                Imgproc.putText(frame,sb.substring(0,50),new Point(dyn_xmin, dyn_ymax + 80),fontface,0.50,red,2);
            }

            //HighGui.imshow("frame", frame);
            //HighGui.waitKey(25);

            if (boxes.size() == 25){

                squares = getSquares(boxes);
                update_row_dist(symbols);
                sym_row_norm = gen_row_norm(sym_row_dist);
                update_col_dist(symbols);
                sym_col_norm = gen_col_norm(sym_col_dist);
                update_number_dist(numbers);
                num_norm = gen_num_norm(n_dist);

                row_metric = sym_row_norm.stream()
                    .map(s -> Double.toString(Collections.max(s)))
                    //.map(Object::toString)
                    .collect(Collectors.joining(","));
                col_metric = sym_col_norm.stream()
                    .map(s -> Double.toString(Collections.max(s)))
                    //.map(Object::toString)
                    .collect(Collectors.joining(","));
                num_metric = num_norm.stream()
                    .map(s -> Double.toString(Collections.max(s)))
                    //.map(Object::toString)
                    .collect(Collectors.joining(","));

                row_metric_1 = Arrays.stream(sym_row_dist).flatMapToInt(Arrays::stream).sum();
                col_metric_1 = Arrays.stream(sym_col_dist).flatMapToInt(Arrays::stream).sum();
                num_metric_1 = Arrays.stream(n_dist).flatMapToInt(Arrays::stream).sum();
                   
                if ( sym_row_norm.stream().map(s -> Collections.max(s)).filter(s -> s < 75).count() == 0 &&
                    sym_row_norm.stream().map(s -> Collections.max(s)).filter(s -> s < 75).count() == 0 &&
                    num_norm.stream().map(s -> Collections.max(s)).filter(s -> s < 75).count() == 0 &&
                    Arrays.stream(n_dist).flatMapToInt(Arrays::stream).sum() > 50 &&
                    Process == true){

                    //System.out.println("confidence criteria met");

                    PrintWriter pw = new PrintWriter(csvFile);
                    //StringBuilder sb = new StringBuilder();
                    sb.setLength(0);

                    num_weighted = get_num_weighted(n_dist);

                    int[][] puzzle = new int[5][5];
                    for (int row = 0; row < 5; row++){
                        for (int col = 0; col < 5; col++){
                            puzzle[row][col] = num_weighted.get(row * 5 + col);
                            sb.append(puzzle[row][col]);
                            if (col < 4){
                                sb.append(',');
                            } 
                        }
                        sb.append('\n');
                    }
                    System.out.println(Arrays.deepToString(puzzle));

                    weighted = get_weighted(sym_row_dist);

                    int[][] arr = new int[5][4];
                    for (int row = 0; row < 5; row++){
                        for (int col = 0; col < 4; col++){
                            arr[row][col] = weighted.get(row * 4 + col);
                            sb.append(arr[row][col]);
                            if (col < 3){
                                sb.append(',');
                            } 
                        }
                        sb.append('\n');
                    }
                    System.out.println(Arrays.deepToString(arr));

                    weighted = get_weighted(sym_col_dist);

                    int[][] arr1 = new int[5][4];
                    for (int row = 0; row < 5; row++){
                        for (int col = 0; col < 4; col++){
                            arr1[row][col] = weighted.get(row * 4 + col);
                            sb.append(arr1[row][col]);
                            if (col < 3){
                                sb.append(',');
                            } 
                        }
                        sb.append('\n');
                    }
                    System.out.println(Arrays.deepToString(arr1));

                    pw.write(sb.toString());
                    pw.close();

                    //Imgproc.putText(frame,sb.toString(),new Point(dyn_xmin, dyn_ymax + 80),fontface,0.75,red,1);

                    HttpClient client = HttpClient.newHttpClient();

                    HttpRequest request = HttpRequest.newBuilder()
                        //.uri(URI.create("http://127.0.0.1:8000/"))
                        .uri(URI.create("https://futoshiki-solver.herokuapp.com/"))
                        .POST(BodyPublishers.ofString(sb.toString()))
                        .build();
        
                    HttpResponse<String> response =
                        client.send(request, BodyHandlers.ofString());

                    sol = response.body();
                    
                    System.out.println(sol);

                    if (sol.length() > 0){
                        Process = false;
                    }

                    for (int i = 0; i < sym_row_dist.length; i++) { for (int j = 0; j < sym_row_dist[i].length; j++) { sym_row_dist[i][j] = 0; } }
                    for (int i = 0; i < sym_col_dist.length; i++) { for (int j = 0; j < sym_col_dist[i].length; j++) { sym_col_dist[i][j] = 0; } }
                    for (int i = 0; i < n_dist.length; i++) { for (int j = 0; j < n_dist[i].length; j++) { n_dist[i][j] = 0; } }
                }

            } else {
                //System.out.println("test");
                for (int i = 0; i < sym_row_dist.length; i++) { for (int j = 0; j < sym_row_dist[i].length; j++) { sym_row_dist[i][j] = 0; } }
                for (int i = 0; i < sym_col_dist.length; i++) { for (int j = 0; j < sym_col_dist[i].length; j++) { sym_col_dist[i][j] = 0; } }
                for (int i = 0; i < n_dist.length; i++) { for (int j = 0; j < n_dist[i].length; j++) { n_dist[i][j] = 0; } }
                Process = true;

            }

            HighGui.imshow("frame", frame);
            HighGui.waitKey(25);
            
        }  

    }

    public static List<Rect> getSquares(List<Rect> boxes) {
        // CB 25-11-21
        // method takes 25 boxes and maps to 25 squares where square 0 is top left and square 24 is bottom right
        // boxes have no inherent or reliable ordering when deteced using opencv find contours

        List<Rect> squares = new ArrayList<Rect>();
        List<Rect> row = new ArrayList<Rect>();

        //sort by y i.e. row order
        boxes.sort(Comparator.comparing(point -> point.y));

        // grab each row of 5, then order by x, finally add to new list
        for (int i = 0; i <= 20; i += 5){
            row = boxes.subList(i, i + 5);
            row.sort(Comparator.comparing(point -> point.x));
            for (Rect r : row) {
                squares.add(r);
            }
        }
        return squares;
    }

    public static void update_row_dist(ArrayList<ArrayList<Object>> symbols){
        for (ArrayList<Object> symbol:symbols){
            for (int row = 0; row < 5; row++){
                for (int col = 0; col < 4; col++){
                    int squareID = row * 5 + col;
                    Rect r = (Rect)symbol.get(0);
                    if (r.x > (squares.get(squareID).x + squares.get(squareID).width) &&
                        r.x < squares.get(squareID + 1).x &&
                        r.y > squares.get(squareID).y &&
                        r.y < (squares.get(squareID).y + squares.get(squareID).height)
                        ){
                            String s = (String)symbol.get(1);
                            if (s == "L"){
                                //System.out.println(s);
                                sym_row_dist[row * 4 + col][0] += 1;
                            }
                            else{
                                //System.out.println(s);
                                sym_row_dist[row * 4 + col][1] += 1;
                            }
                    }
                }
            }
        }
    }

    public static void update_col_dist(ArrayList<ArrayList<Object>> symbols){
        for (ArrayList<Object> symbol:symbols){
            for (int col = 0; col < 5; col++){
                for (int row = 0; row < 4; row++){
                    int squareID = row * 5 + col;
                    Rect r = (Rect)symbol.get(0);
                    //if (symbol[0] > squares[square][0]) and (symbol[0] < (squares[square][0] + squares[square][2])):
                    //if (symbol[1] > (squares[square][1] + squares[square][3])) and symbol[1] < squares[square + 5][1]:
                    if (r.x > squares.get(squareID).x && r.x < (squares.get(squareID).x + squares.get(squareID).width) &&
                        r.y > (squares.get(squareID).y + squares.get(squareID).height) && r.y < squares.get(squareID + 5).y
                        ){
                            String s = (String)symbol.get(1);
                            if (s == "U"){
                                //System.out.println(s);
                                sym_col_dist[col * 4 + row][0] += 1;
                            }
                            else{
                                //System.out.println(s);
                                sym_col_dist[col * 4 + row][1] += 1;
                            }
                    }
                }
            }
        }
    }

    public static void update_number_dist(ArrayList<ArrayList<Object>> numbers){
        for (ArrayList<Object> number:numbers){
            for (int row = 0; row < 5; row++){
                for (int col = 0; col < 5; col++){
                    int squareID = row * 5 + col;
                    Rect r = (Rect)number.get(0);
                    if (r.x < (squares.get(squareID).x + squares.get(squareID).width) && r.x > squares.get(squareID ).x &&
                        r.y > squares.get(squareID).y && r.y < (squares.get(squareID).y + squares.get(squareID).height)
                        ){
                            String s = (String)number.get(1);
                            int n = Integer.parseInt(s);
                            //System.out.println(s);
                            n_dist[squareID][n] += 1;
                        
                    }
                }
            }
        }
    }

    public static ArrayList<ArrayList<Double>> gen_row_norm(int sym_row_dist[][]){
        ArrayList<ArrayList<Double>> sym_norm = new ArrayList<ArrayList<Double>>();
        for (int row = 0; row < 5; row++){
            for (int col = 0; col < 4; col++){
                int s1 = sym_row_dist[row * 4 + col][0];
                int s2 = sym_row_dist[row * 4 + col][1];
                if ( s1 > 0 || s2 > 0 ){
                    ArrayList<Double> test = new ArrayList<Double>();
                    double d1 = s1 * 100 / (s1 + s2);
                    test.add(d1);
                    double d2 = s2 * 100 / (s1 + s2);
                    test.add(d2);
                    sym_norm.add(test);
                }
            }
        }
        return sym_norm;
    }

    public static ArrayList<ArrayList<Double>> gen_col_norm(int sym_col_dist[][]){
        ArrayList<ArrayList<Double>> sym_col_norm = new ArrayList<ArrayList<Double>>();
        for (int row = 0; row < 5; row++){
            for (int col = 0; col < 4; col++){
                int s1 = sym_col_dist[row * 4 + col][0];
                int s2 = sym_col_dist[row * 4 + col][1];
                if ( s1 > 0 || s2 > 0 ){
                    ArrayList<Double> test = new ArrayList<Double>();
                    double d1 = s1 * 100 / (s1 + s2);
                    test.add(d1);
                    double d2 = s2 * 100 / (s1 + s2);
                    test.add(d2);
                    sym_col_norm.add(test);
                }
            }
        }
        return sym_col_norm;
    }
    
    

    public static ArrayList<ArrayList<Double>> gen_num_norm(int n_dist[][]){
        ArrayList<ArrayList<Double>> num_norm = new ArrayList<ArrayList<Double>>();
        for (int row = 0; row < 25; row++){
            int sum = 0;
            for (int col = 0; col < 5; col++){
                sum += n_dist[row][col];
            }
            if ( sum > 0 ){
                ArrayList<Double> test = new ArrayList<Double>();
                for (int col = 0; col < 5; col++){
                    // normalise each value using total number
                    double d1 = n_dist[row][col] * 100 / sum;
                    test.add(d1);
                }
                num_norm.add(test);
            }
        }

        return num_norm;
    }


    public static List<Integer> get_weighted(int sym_row_dist[][]){
        List<Integer> weighted = Arrays.stream(sym_row_dist)
        //.map(m -> Arrays.stream(m).reduce((a,b) -> m[a]<m[b]? a: b))
        .map(m -> {
            Integer result = 0;
            if (m[0] > m[1]){
                result = 1;
            } else if (m[0] < m[1]){
                result = 2;
            }
            return result;
        })
        .collect(Collectors.toList());

        return weighted;
    }

    public static List<Integer> get_num_weighted(int n_dist[][]){
        List<Integer> num_weighted = Arrays.stream(n_dist)
        //.map(m -> Arrays.stream(m).reduce((a,b) -> m[a]<m[b]? a: b))
        .map(m -> {
            int index_of_max = IntStream.range(0, 5)
                .reduce(0,(a,b)->m[a]<m[b]? b: a);
                //.ifPresent(ix->System.out.println("Index "+ix+", value "+m[ix]));
            return index_of_max;
        })
        .collect(Collectors.toList());
        return num_weighted;
    }
}
