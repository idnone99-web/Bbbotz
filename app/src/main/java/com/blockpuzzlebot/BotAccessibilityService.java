package com.blockpuzzlebot;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Path;
import android.hardware.HardwareBuffer;
import android.os.Build;
import android.os.Handler;
import android.view.Display;
import android.view.accessibility.AccessibilityEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class BotAccessibilityService extends AccessibilityService {
    public static volatile boolean running = false;
    private final Handler h = new Handler();
    private boolean busy = false;
    private static final int N=8;

    @Override public void onAccessibilityEvent(AccessibilityEvent e) { if(running && !busy) schedule(); }
    @Override public void onInterrupt() { running=false; }
    @Override protected void onServiceConnected() { super.onServiceConnected(); if(running) schedule(); }
    private void schedule(){ busy=true; h.postDelayed(this::shot, 450); }

    private void shot(){
        if(!running){busy=false;return;}
        if(Build.VERSION.SDK_INT < 30){busy=false;return;}
        takeScreenshot(Display.DEFAULT_DISPLAY, Executors.newSingleThreadExecutor(), new TakeScreenshotCallback(){
            @Override public void onSuccess(ScreenshotResult r){
                Bitmap b=null; HardwareBuffer hb=r.getHardwareBuffer();
                try { b=Bitmap.wrapHardwareBuffer(hb, r.getColorSpace()); if(b!=null){ Bitmap copy=b.copy(Bitmap.Config.ARGB_8888,false); b.recycle(); analyze(copy); copy.recycle(); } }
                catch(Exception ignored){} finally { hb.close(); }
            }
            @Override public void onFailure(int errorCode){ busy=false; if(running) schedule(); }
        });
    }

    // Coordinates are normalized from the supplied 1080x2400 POCO X7 Pro screenshot.
    private void analyze(Bitmap b){
        float sx=b.getWidth()/1080f, sy=b.getHeight()/2400f;
        int bx=139, by=792, pitch=101, cell=74;
        boolean[][] board=new boolean[N][N];
        for(int r=0;r<N;r++) for(int c=0;c<N;c++) board[r][c]=lightCell(b,(int)((bx+c*pitch+pitch/2)*sx),(int)((by+r*pitch+pitch/2)*sy),(int)(cell*sx));
        List<Piece> pieces=new ArrayList<>();
        pieces.add(readPiece(b,215,1874,52,sx,sy));
        pieces.add(readPiece(b,518,1874,52,sx,sy));
        pieces.add(readPiece(b,737,1874,52,sx,sy));
        if(pieces.size()!=3 || pieces.stream().anyMatch(p->p.cells.isEmpty())) { busy=false; if(running)schedule(); return; }
        Move best=choose(board,pieces);
        if(best==null){ busy=false; if(running)schedule(); return; }
        performDrag(best,pieces.get(best.piece));
    }

    private boolean lightCell(Bitmap b,int x,int y,int size){
        int half=Math.max(4,size/4), count=0, light=0;
        for(int yy=y-half;yy<=y+half;yy+=3) for(int xx=x-half;xx<=x+half;xx+=3){
            if(xx<0||yy<0||xx>=b.getWidth()||yy>=b.getHeight()) continue;
            int p=b.getPixel(xx,yy); int lum=(Color.red(p)+Color.green(p)+Color.blue(p))/3;
            count++; if(lum>105 && Color.red(p)>Color.blue(p)*1.15f) light++;
        }
        return count>0 && light/(float)count>0.28f;
    }

    static class Piece { List<int[]> cells=new ArrayList<>(); int sx,sy; }
    private Piece readPiece(Bitmap b,int ox,int oy,int pitch,float sx,float sy){
        Piece p=new Piece(); p.sx=ox; p.sy=oy;
        // Search a 5x3 logical area around the expected piece center. Threshold is intentionally conservative.
        for(int rr=-1;rr<=2;rr++) for(int cc=-2;cc<=3;cc++){
            int x=Math.round((ox+cc*pitch)*sx), y=Math.round((oy+rr*pitch)*sy);
            if(lightCell(b,x,y,(int)(36*sx))) p.cells.add(new int[]{rr+1,cc+2});
        }
        if(!p.cells.isEmpty()){
            int minr=99,minc=99; for(int[] q:p.cells){minr=Math.min(minr,q[0]);minc=Math.min(minc,q[1]);}
            for(int[] q:p.cells){q[0]-=minr;q[1]-=minc;}
        }
        return p;
    }

    static class Move { int piece,r,c,score; }
    private Move choose(boolean[][] base,List<Piece> ps){
        Move best=null;
        for(int pi=0;pi<ps.size();pi++){
            Piece p=ps.get(pi);
            for(int r=0;r<N;r++) for(int c=0;c<N;c++){
                boolean[][] a=copy(base); if(!place(a,p,r,c)) continue;
                int cleared=clear(a); int empty=countEmpty(a); int holes=holes(a);
                int score=cleared*10000 + empty*8 - holes*12 - edgePenalty(a);
                if(best==null||score>best.score){best=new Move();best.piece=pi;best.r=r;best.c=c;best.score=score;}
            }
        }
        return best;
    }
    private boolean place(boolean[][] a,Piece p,int r,int c){
        for(int[] q:p.cells){int rr=r+q[0],cc=c+q[1];if(rr<0||cc<0||rr>=N||cc>=N||a[rr][cc])return false;}
        for(int[] q:p.cells)a[r+q[0]][c+q[1]]=true; return true;
    }
    private int clear(boolean[][] a){int n=0;for(int r=0;r<N;r++){boolean ok=true;for(int c=0;c<N;c++)ok&=a[r][c];if(ok){n++;for(int c=0;c<N;c++)a[r][c]=false;}}
        for(int c=0;c<N;c++){boolean ok=true;for(int r=0;r<N;r++)ok&=a[r][c];if(ok){n++;for(int r=0;r<N;r++)a[r][c]=false;}}return n;}
    private int countEmpty(boolean[][]a){int n=0;for(boolean[]r:a)for(boolean v:r)if(!v)n++;return n;}
    private int holes(boolean[][]a){int n=0;for(int r=1;r<N-1;r++)for(int c=1;c<N-1;c++)if(!a[r][c]&&a[r-1][c]&&a[r+1][c]&&a[r][c-1]&&a[r][c+1])n++;return n;}
    private int edgePenalty(boolean[][]a){int n=0;for(int r=0;r<N;r++)for(int c=0;c<N;c++)if(a[r][c]&&(r==0||c==0||r==N-1||c==N-1))n++;return n;}
    private boolean[][] copy(boolean[][]a){boolean[][]b=new boolean[N][N];for(int i=0;i<N;i++)System.arraycopy(a[i],0,b[i],0,N);return b;}

    private void performDrag(Move m,Piece p){
        float sx=getResources().getDisplayMetrics().widthPixels/1080f;
        float sy=getResources().getDisplayMetrics().heightPixels/2400f;
        float startX=(p.sx)*sx, startY=p.sy*sy;
        float endX=(139+m.c*101+37)*sx, endY=(792+m.r*101+37)*sy;
        Path path=new Path(); path.moveTo(startX,startY); path.lineTo(endX,endY);
        GestureDescription gd=new GestureDescription.Builder().addStroke(new GestureDescription.StrokeDescription(path,0,450)).build();
        dispatchGesture(gd,new GestureResultCallback(){@Override public void onCompleted(GestureDescription g){busy=false;if(running)schedule();}@Override public void onCancelled(GestureDescription g){busy=false;if(running)schedule();}},h);
    }
}
