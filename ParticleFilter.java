import java.lang.Math;
import java.util.Scanner;
public class ParticleFilter {
    //获取fingerprint,目前是二维的地图，依据实际可扩展为三维地图
    public double[][] getFingerprint(){
        double[][] finger = {{0,0,0,0,0,0,0,0,0,0.4},
                {76.4,0,0.2,0,0,77.2,65.2,0,0,0},
                {75.8,0,0,2,0,62.2,0,0,48.4,31.2},
                {73.8,79.4,78.8,0,58,78.8,37.6,77.8,65.2,74.4},
                {0,76,77.8,45.6,3,22.6,79.2,72.2,46.4,78.4}};
        return finger;
    }
    //初始化粒子,全局均匀分布
    public particle[][] init(){
        double[][] finger = this.getFingerprint();
        int line = finger.length;
        int col = finger[0].length;
        particle[][] pt = new particle[line*col][];
        for(int i=0;i<line*col;i++)
            pt[i] = new particle[5];
        for(int i=0;i<line;i++){
            for(int j=0;j<col;j++){
                for(int k=0;k<5;k++){
                    pt[i*col+j][k] = new particle();
                    pt[i*col+j][k].setPosition(i,j);
                    pt[i*col+j][k].setPRR(finger[i][j]);
                }
                pt[i*col+j][0].setDirection(-1,0);//向上移动粒子
                pt[i*col+j][1].setDirection(1,0);//向下移动粒子
                pt[i*col+j][2].setDirection(0,-1);//向左移动粒子
                pt[i*col+j][3].setDirection(0,1);//向右移动粒子
                pt[i*col+j][4].setDirection(0,0);//不移动粒子
            }
        }
        return pt;
    }
    //计算粒子权重
    public particle[][] weightCompute(particle[][] p,double lastPRR,double PRR){
        double[][] finger = this.getFingerprint();
        double weight_MAX = 0;
        for(int i=0;i<p.length;i++){
            for(int j=0;j<p[0].length;j++){
                if(p[i][j].flag == 1){
                    p[i][j].setLastPRR(p[i][j].PRR);
                    int x,y;
                    x = p[i][j].i+p[i][j].x;
                    y = p[i][j].j+p[i][j].y;
                    if(0<=x&&x<finger.length&&0<=y&&y<finger[0].length){
                        p[i][j].setPosition(x,y);
                        p[i][j].setPRR(finger[x][y]);
                    }
                    double weight;
                    double a = p[i][j].PRR-p[i][j].lastPRR;
                    double b = PRR-lastPRR;
                    weight = 1-Math.abs(a-b)/100;//权重计算方式待优化
                    weight += p[i][j].weight;
                    p[i][j].setWeight(weight);
                    if(weight>weight_MAX)
                        weight_MAX = weight;
                }
            }
        }
        System.out.println(weight_MAX);
        predictPosition(p,weight_MAX);
        return p;
    }
    //粒子重采样
    public particle[][] reSample(particle[][] p,int time){
        double threshold = 0.5;//过滤低权重粒子
        double[][] finger = this.getFingerprint();
        int line = finger.length;
        int col = finger[0].length;
        double[][] state = new double[line][col];
        for(int i=0;i<line;i++){
            for(int j=0;j<col;j++)
                state[i][j]  = 0;
        }
        int len = p.length;
        for(int i=0;i<len;i++){
            for(int j=0;j<p[0].length;j++){
                if(p[i][j].weight>=(threshold*time)&&p[i][j].flag==1){
                    int x = p[i][j].i;
                    int y = p[i][j].j;
                    if(p[i][j].weight>state[x][y])
                        state[x][y] = p[i][j].weight;//记录此区域最大的权重值
                }
            }
        }
        particle[][] pt = new particle[line*col][5];
        int index = 0;
        for(int i=0;i<line;i++){
            for(int j=0;j<col;j++){
                if(state[i][j]>0){
                    for(int k=0;k<pt[0].length;k++){
                        pt[index][k] = new particle();
                        pt[index][k].setPosition(i,j);
                        pt[index][k].setPRR(finger[i][j]);
                        pt[index][k].setWeight(state[i][j]);
                    }
                    pt[index][0].setDirection(-1,0);
                    pt[index][1].setDirection(1,0);
                    pt[index][2].setDirection(0,-1);
                    pt[index][3].setDirection(0,1);
                    pt[index][4].setDirection(0,0);
                    index++;
                }
            }
        }
        for(int i=index;i<pt.length;i++){
            for(int j=0;j<pt[0].length;j++){
                pt[i][j] = new particle();
                pt[i][j].setFlag(0);
            }
        }
        return pt;
    }
    //坐标预测
    public void predictPosition(particle[][] p,double weight_MAX){
        double[][] finger = this.getFingerprint();
        int line = finger.length;
        int col = finger[0].length;
        int[][] state = new int[line][col];
        for(int i=0;i<p.length;i++){
            for(int j=0;j<p[0].length;j++){
                if(p[i][j].flag==1&&p[i][j].weight==weight_MAX)
                    state[p[i][j].i][p[i][j].j] = 1;
            }
        }
        for(int i=0;i<line;i++){
            for(int j=0;j<col;j++){
                if(state[i][j]==1)
                    System.out.println("The most likely location is:i="+i+",j="+j);
            }
        }
    }
    public static void main(String[] args){
        ParticleFilter pf = new ParticleFilter();
        particle[][] pt = pf.init();
        double lastPRR,PRR;
        Scanner sc = new Scanner(System.in);
        lastPRR = sc.nextDouble();
        int time = 0;
        while(true){
            time++;
            PRR = sc.nextDouble();
            if(PRR<0)
                return;
            pt = pf.weightCompute(pt,lastPRR,PRR);
            pt = pf.reSample(pt,time);
            lastPRR = PRR;
        }
    }
}

class particle{
    int i,j,flag;    //当前点位置和是点是否有效
    int x,y;    //方向
    double lastPRR,PRR,weight;   //上一位置和当前位置的包接受率,权重
    public particle(){
        this.weight = 0;
        this.lastPRR = 0;
        this.PRR =  0;
        this.weight = 0;
        this.flag = 1;
    }
    public void setPosition(int i,int j){
        this.i = i;
        this.j = j;
    }
    public void setLastPRR(double lastPRR){
        this.lastPRR = lastPRR;
    }
    public void setPRR(double PRR){
        this.PRR = PRR;
    }
    public void setWeight(double weight){
        this.weight = weight;
    }
    public void setDirection(int x,int y){
        this.x = x;
        this.y = y;
    }
    public void setFlag(int flag){
        this.flag = flag;
    }
}