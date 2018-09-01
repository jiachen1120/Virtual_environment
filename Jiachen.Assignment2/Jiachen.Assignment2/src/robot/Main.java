// ==========================================================================
// $Id: Main.java,v 1.4 2016/10/14 02:34:29 jlang Exp $
// ELG5124/CSI5151 Robot Arm Kinematics
// ==========================================================================
// (C)opyright:
//
//   Jochen Lang
//   EECS, University of Ottawa
//   800 King Edward Ave.
//   Ottawa, On., K1N 6N5
//   Canada. 
//   http://www.eecs.uottawa.ca
// 
// Creator: jlang (Jochen Lang)
// Email:   jlang@eecs.uottawa.ca
// ==========================================================================
// $Log: Main.java,v $
// ==========================================================================
package robot;

import com.jme3.app.SimpleApplication;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Eigen3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Sphere;
import com.jme3.system.AppSettings;
import com.jme3.scene.shape.Line;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.util.BufferUtils;
import com.jme3.input.controls.AnalogListener;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.FastMath;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Cylinder;

public class Main extends SimpleApplication {

    private Geometry line;
    private Geometry target;
    private Geometry lArm;
    private Geometry uArm;
    private Geometry bJoint;
    private Geometry aJoint;
    private Node sObject;
    private Node bNode;
    private Node bNode0;
    private Node aNode;
    private int nodeNum;
    private float angle0;
    private float angle1;
    private float angle2;
    private Boolean toggle=true;
            
    public static void main(String[] args) {
        Main app = new Main();
        // app.setShowSettings(false);
        AppSettings settings = new AppSettings(true);
        /*
        settings.put("Width", 640);
        settings.put("Height", 480);
        */
        settings.put("Title", "ELG5124 Robot Arm");
        // VSynch
        // settings.put("VSync", true)
        // Anti-Aliasing
        // settings.put("Samples", 4);
        // Initialize sphere control        
        app.setSettings(settings);
        app.start();
    }
    
    private Vector3f startP; 
    private Vector3f endP;
    private Vector3f startIK;
    private Vector3f endIK;
    Vector3f [] vertices;

    /**
     * Simple analoglistener which takes the camera-based directions and adds it
     * to the local translation of the target. Implicitly assumes that the target is
     * directly attached to scene root.
     */
    final private AnalogListener analogListener;

    
    private final ActionListener actionListener;

    
    public Main() {
        this.actionListener = new ActionListener() {    
            @Override
            public void onAction(String name, boolean keyPressed, float tpf) {
                if (name.equals("Next") && !keyPressed) {
                    // Ray from target position at last time step to current.
                    // Reset our line drawing
                    startP.set(endP);
                    // and redraw
                    updateLine();
                }
                if (name.equals("Toggle") && !keyPressed) {
                    if (nodeNum == 2) {
                    nodeNum = 0;
                    }
                    else {
                    nodeNum ++;
                    }
                }
                    
            }
        };
        
        this.analogListener = new AnalogListener() {
            @Override
            public void onAnalog(String name, float value, float tpf) {
                value *= 10.0;
                // find forward/backward direction and scale it down
                Vector3f camDir = cam.getDirection().clone().multLocal(value);
                // find right/left direction
                Vector3f camLeft = cam.getLeft().clone().multLocal(value);
                // find up/down direction
                Vector3f camUp = cam.getUp().clone().multLocal(value);
                boolean pChange = false;
                if (name.equals("Left")) {
                    Vector3f v = target.getLocalTranslation();
                    target.setLocalTranslation(v.add(camLeft));
                    pChange = true;
                }
                if (name.equals("Right")) {
                    Vector3f v = target.getLocalTranslation();
                    target.setLocalTranslation(v.add(camLeft.negateLocal()));
                    pChange = true;
                }
                if (name.equals("Forward")) {
                    Vector3f v = target.getLocalTranslation();
                    target.setLocalTranslation(v.add(camDir));
                    pChange = true;
                }
                if (name.equals("Back")) {
                    Vector3f v = target.getLocalTranslation();
                    target.setLocalTranslation(v.add(camDir.negateLocal()));
                    pChange = true;
                }
                if (name.equals("Up")) {
                    Vector3f v = target.getLocalTranslation();
                    target.setLocalTranslation(v.add(camUp));
                    pChange = true;
                }
                if (name.equals("Down")) {
                    Vector3f v = target.getLocalTranslation();
                    target.setLocalTranslation(v.add(camUp.negateLocal()));
                    pChange = true;
                }
                if (pChange) updateLine();
                
                if (toggle == true) {
                   if (name.equals("Clockwise")){
                       if (nodeNum == 0){
                           bNode0.rotate(0, value*speed, 0);
                       }
                       if (nodeNum == 1){
                           bNode.rotate(0, 0, value*speed);
                       }
                       if (nodeNum == 2){
                           aNode.rotate(0, 0, value*speed);
                       }
                   }
                   if (name.equals("Anticlockwise")){
                       if (nodeNum == 0){
                           bNode0.rotate(0, -value*speed, 0);
                       }
                       if (nodeNum == 1){
                           bNode.rotate(0, 0, -value*speed);
                       }
                       if (nodeNum == 2){
                           aNode.rotate(0, 0, -value*speed);
                       }
                   }
                }
            }
        };
    }

    @Override
    public void simpleInitApp() {
        // Left mouse button press to rotate camera
        flyCam.setDragToRotate(true);
        // Do not display stats or fps
        setDisplayFps(false);
        setDisplayStatView(false);
        // move the camera back (10 is the default)
        // cam.setLocation(new Vector3f(0f, 0f, 5.0f));
        // also: cam.setRotation(Quaternion)

        angle0 = 0f;
        angle1 = (-90)*FastMath.DEG_TO_RAD;
        angle2 = (90)*FastMath.DEG_TO_RAD;
        
        // Generate the robot - starting with a box for the base
        sObject = new Node();
        Geometry base = new Geometry("Base", new Box(0.5f, 0.5f, 0.5f));
        base.setLocalTranslation(-2.05f, -2.5f, 0.0f);
        Material matBase = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        // Use transparency - just to make sure we can always see the target
        matBase.setColor("Color", new ColorRGBA( 0.7f, 0.7f, 0.7f, 0.5f)); // silver'ish
        matBase.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
        base.setMaterial(matBase);
        base.setQueueBucket(RenderQueue.Bucket.Transparent);
        
        sObject.attachChild(base);
        rootNode.attachChild(sObject);
        
        // Generate two spheres as the joints
        bNode0 = new Node(); 
        bNode0.setLocalTranslation(-2.05f, -2f, 0.0f);
        bNode = new Node(); 
        bNode.setLocalTranslation(0f, 0f, 0.0f);
        aNode = new Node();
        aNode.setLocalTranslation(0f, 2f, 0.0f);
        Node aNode0 = new Node();
        bJoint = new Geometry("bJoint", new Sphere(6, 12, 0.32f));
        bJoint.setLocalTranslation(0f, 0f, 0.0f);
        Material matJoint = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matJoint.setColor("Color", ColorRGBA.Yellow);
        bJoint.setMaterial(matJoint);
        
        aJoint = new Geometry("aJoint", new Sphere(6, 12, 0.32f));
        aJoint.setLocalTranslation(0f, 0.0f, 0.0f);
        matJoint.setColor("Color", ColorRGBA.Yellow);
        aJoint.setMaterial(matJoint);
        
        // Generate a cylinder as the lower arm
        lArm = new Geometry("lArm", new Cylinder(10,10,0.35f,2f,true,false));
        lArm.setLocalTranslation(0f, 1f, 0.0f);
        Quaternion pitch90 = new Quaternion();
        pitch90.fromAngleAxis(FastMath.PI / 2, new Vector3f(1,0,0));
        lArm.setLocalRotation(pitch90);
        lArm.setMaterial(matBase);
        lArm.setQueueBucket(RenderQueue.Bucket.Transparent);
        
        // Generate a cylinder as the upper arm
        uArm = new Geometry("uArm", new Cylinder(10,10,0.35f,2f,true,false));
        uArm.setLocalTranslation(1f, 0.0f, 0.0f);
        Quaternion pitch90y = new Quaternion();
        pitch90y.fromAngleAxis(FastMath.PI / 2, new Vector3f(0,1,0));
        uArm.setLocalRotation(pitch90y);
        uArm.setMaterial(matBase);
        uArm.setQueueBucket(RenderQueue.Bucket.Transparent);
        
        // Generate two boxes as the claws
        Node clawsNode = new Node();
        Node cNode0 = new Node();
        Geometry uClaw = new Geometry("uClaw", new Box(0.3f, 0.12f, 0.3f));
        uClaw.setLocalTranslation(1.95f, 0.175f, 0.0f);
        uClaw.setMaterial(matBase);
        uClaw.setQueueBucket(RenderQueue.Bucket.Transparent);
        uClaw.rotate(0f, 0f, .5f);
        
        Geometry lClaw = new Geometry("lClaw", new Box(0.3f, 0.12f, 0.3f));
        lClaw.setLocalTranslation(1.95f, -0.175f, 0.0f);
        lClaw.setMaterial(matBase);
        lClaw.setQueueBucket(RenderQueue.Bucket.Transparent);
        lClaw.rotate(0f, 0f, -0.5f);
        
        // Generate a sphere as a symbol for the target point
        Node tNode = new Node();
        target = new Geometry("Sphere", new Sphere(6, 12, 0.1f));
        Material matSphere = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matSphere.setColor("Color", ColorRGBA.Red);
        target.setMaterial(matSphere);

        rootNode.attachChild(target);
        
        //connect
        clawsNode.attachChild(uClaw);
        clawsNode.attachChild(lClaw);
        cNode0.attachChild(clawsNode);
        cNode0.attachChild(uArm);
        aNode.attachChild(cNode0);
        aNode.attachChild(aJoint);
        aNode0.attachChild(aNode);
        aNode0.attachChild(lArm);
        bNode.attachChild(aNode0);
        bNode.attachChild(bJoint);
        bNode0.attachChild(bNode);
        
        //rootNode.attachChild(bNode);
        rootNode.attachChild(bNode0);

        // Set up line drawing from last position to current position of target
        // We artifically create a time step based on the number of interactions
        startP = new Vector3f(target.getLocalTranslation());
        endP = new Vector3f(target.getLocalTranslation());
        startIK = new Vector3f(0f,0f,0f);
        endIK = new Vector3f(0f,0f,0f);
        vertices = new Vector3f[]{startP,endP};
        Line ln = new Line(startP,endP);
        ln.setLineWidth(2);
        line = new Geometry("line", ln);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Green);
        line.setMaterial(mat);
        
        rootNode.attachChild(line);

        /** Set up interaction keys */
        setUpKeys();

        // Can be used to change mapping of mouse/keys to camera behaviour
        /*
         if (inputManager != null) {
         inputManager.deleteMapping("FLYCAM_RotateDrag");
         flyCam.setDragToRotate(true);
         inputManager.addMapping("FLYCAM_RotateDrag", new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));
         inputManager.addListener(flyCam, "FLYCAM_RotateDrag");
         }
         */
    }

    private void setUpKeys() {
        inputManager.addMapping("Left", new KeyTrigger(KeyInput.KEY_LEFT));
        inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_RIGHT));
        inputManager.addMapping("Up", new KeyTrigger(KeyInput.KEY_UP));
        inputManager.addMapping("Down", new KeyTrigger(KeyInput.KEY_DOWN));
        inputManager.addMapping("Forward", new KeyTrigger(KeyInput.KEY_PGUP));
        inputManager.addMapping("Back", new KeyTrigger(KeyInput.KEY_PGDN));
        inputManager.addMapping("Anticlockwise", new KeyTrigger(KeyInput.KEY_MINUS));
        inputManager.addMapping("Clockwise", new KeyTrigger(KeyInput.KEY_EQUALS));
        inputManager.addMapping("Back", new KeyTrigger(KeyInput.KEY_PGDN));
        inputManager.addListener(analogListener,
                "Left", "Right", "Up", "Down", "Forward", "Back","Anticlockwise", "Clockwise");
        inputManager.addMapping("Next", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("Toggle", new KeyTrigger(KeyInput.KEY_J));
        inputManager.addListener(actionListener, "Next", "Toggle", "Joint0", "Joint1", "Joint2");
    }
    
    public Jama.Matrix UnaryNotZeroElement (Jama.Matrix x) {
        double[][] array = x.getArray();
        for (int i = 0; i < array.length; i++) {
            for (int j = 0; j < array[i].length; j++){
                if (array[i][j] > 0.1) {
                    array[i][j] = 1.0/array[i][j];
                }
                else{
                    array[i][j] = 0;
                }
            }
        }
        Jama.Matrix xnew = new Jama.Matrix(array);
        return(xnew);
    }
    
    // Helper routine to update line
    void updateLine() {
        Mesh m = line.getMesh();
        endP.set(target.getLocalTranslation());
        m.setBuffer(Type.Position, 3, BufferUtils.createFloatBuffer(vertices));
        m.updateBound();
    }    
    
    // Update the position every sec
    @Override
    public void simpleUpdate(float tpf) {
        double[][] dm = new double[3][3];
        double[][] tArray = new double[3][1];
        
        endIK = target.getLocalTranslation();
        Vector3f v = new Vector3f(target.getLocalTranslation());
        
        if (endIK.distance(startIK) > 0.001f){
        tArray[0][0] = endIK.x - startIK.x;
        tArray[1][0] = endIK.y - startIK.y;
        tArray[2][0] = endIK.z - startIK.z;
        Jama.Matrix tM = new Jama.Matrix(tArray);

        float c0 = FastMath.cos(angle0);
        float c1 = FastMath.cos(angle1);
        float c2 = FastMath.cos(angle2);
        float s0 = FastMath.sin(angle0);
        float s1 = FastMath.sin(angle1);
        float s2 = FastMath.sin(angle2);
        dm[0][0] = -2*s0*c1 - 2.05*s0*c1*c2 + 2.05*s0*s1*s2;
        dm[0][1] = -2*c0*s1 - 2.05*c0*s1*c2 - 2.05*c0*c1*s2;
        dm[0][2] = -2.05*c0*c1*s2 - 2.05*c0*s1*c2;
        dm[1][0] = 0;
        dm[1][1] = -2*c1 + 2.05*s1*s2 - 2.05*c2*c1;
        dm[1][2] = -2.05*c1*c2 + 2.05*s2*s1;
        dm[2][0] = 2.05*c0*s1*s2 - 2.05*c1*c2*c0 - 2*c1*c0;
        dm[2][1] = 2.05*s0*c1*s2 + 2.05*s1*c2*s0 + 2*s1*s0; 
        dm[2][2] = 2.05*s0*s1*c2 + 2.05*c1*s2*s0;      

        Jama.Matrix Jcb = new Jama.Matrix(dm);
        Jama.SingularValueDecomposition svd = new Jama.SingularValueDecomposition(Jcb);
        Jama.Matrix S = svd.getS();
        Jama.Matrix U = svd.getU();
        Jama.Matrix V = svd.getV();
        Jama.Matrix Sinv = UnaryNotZeroElement (S);
        Jama.Matrix Jcbinv = V.times(Sinv).times(U.transpose());
        Jama.Matrix d_angle = Jcbinv.times(tM);
        
        angle0 += d_angle.get(0, 0);
        angle1 += d_angle.get(1, 0);
        angle2 += d_angle.get(2, 0);
        
        bNode0.rotate(0f, (float)(d_angle.get(0,0)), 0f);
        bNode.rotate(0f, 0f, -(float)(d_angle.get(1,0)));
        aNode.rotate(0f, 0f, -(float)(d_angle.get(2,0)));
        
        startIK = v;
        }
    }
}
