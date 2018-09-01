package BulletBlub;

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.AnimEventListener;
import com.jme3.animation.LoopMode;
import com.jme3.app.SimpleApplication;
import com.jme3.asset.TextureKey;
import com.jme3.audio.AudioData.DataType;
import com.jme3.audio.AudioNode;
import com.jme3.bullet.BulletAppState;
import static com.jme3.bullet.PhysicsSpace.getPhysicsSpace;
import com.jme3.bullet.collision.PhysicsCollisionEvent;
import com.jme3.bullet.collision.PhysicsCollisionListener;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.joints.HingeJoint;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.CameraNode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.CameraControl;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Cylinder;
import com.jme3.scene.shape.Sphere;
import com.jme3.scene.shape.Sphere.TextureMode;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;
import static java.lang.Math.abs;

public class main extends SimpleApplication implements PhysicsCollisionListener, AnimEventListener {

  public static void main(String args[]) {
    main app = new main();
    app.start();
  }

  /** Prepare the Physics Application State (jBullet) */
  private BulletAppState bulletAppState;

  /** Prepare Materials */
  Material wall_mat;
  Material stone_mat;
  Material floor_mat;

  /** Prepare geometries and physical nodes for bricks and cannon balls. */
  private RigidBodyControl    brick_phy;
  private static final Box    box;
  private RigidBodyControl    ball_phy;
  private static final Sphere sphere;
  private RigidBodyControl    floor_phy;
  private static final Box    floor;
  private Geometry  hingeJoint;
  private RigidBodyControl    hingeJoint_phy;
  private HingeJoint joint;
  private HingeJoint jointArray [] = new HingeJoint [3];
  private AnimChannel channel;
  private AnimControl control;
  private RigidBodyControl player_phy;
  private boolean left = false, right = false, up = false, down = false, pageup = false, pagedown = false, Forward = false, Stationary = false, Circle = false;;
  private Vector3f walkDirection = new Vector3f();
  Node player;
  private int forFlag = 1;
  private AudioNode audio_gun;
  private AudioNode audio_nature;

  /** dimensions used for bricks and wall */
  private static final float brickLength = 1.2f;
  private static final float brickWidth  = 2.0f;
  private static final float brickHeight = 0.1f;

  static {
    /** Initialize the cannon ball geometry */
    sphere = new Sphere(32, 32, 0.4f, true, false);
    sphere.setTextureMode(TextureMode.Projected);
    /** Initialize the brick geometry */
    box = new Box(brickLength, brickHeight, brickWidth);
    box.scaleTextureCoordinates(new Vector2f(1f, .5f));
    /** Initialize the floor geometry */
    floor = new Box(4.5f, 4.5f, 4.5f);
    floor.scaleTextureCoordinates(new Vector2f(3, 6));
  }

  @Override
  public void simpleInitApp() {
    /** Set up Physics Game */
    bulletAppState = new BulletAppState();
    stateManager.attach(bulletAppState);
    
    DirectionalLight dl = new DirectionalLight();
    dl.setDirection(new Vector3f(-0.1f, -1f, -1).normalizeLocal());
    rootNode.addLight(dl);
    
    // Setup first view
        // Setup first view
        viewPort.setBackgroundColor(ColorRGBA.Blue);
        cam.setViewPort(0f, 0.6f, 0f, 1f);
        cam.setLocation(new Vector3f(0f, 0f, 12.2812433f));
        flyCam.setDragToRotate(true);
        CameraNode camNode = new CameraNode("Camera Node",cam);
        camNode.setControlDir(CameraControl.ControlDirection.SpatialToCamera);
        rootNode.attachChild(camNode);
        camNode.setLocalTranslation(new Vector3f(0, 0f, 20f));
        camNode.lookAt(new Vector3f(0, 0, 0), Vector3f.UNIT_Y);
        //cam.setRotation(new Quaternion(-0.07680723f, 0.92299235f, -0.2564353f, -0.27645364f));

        // Setup second view
        Camera cam3 = cam.clone();
        cam3.setViewPort(0.6f, 1f, 0f, .5f);
        cam3.setLocation(new Vector3f(0f, 15f, 0f));
        cam3.setRotation(new Quaternion().fromAngleAxis(FastMath.HALF_PI, new Vector3f(1,0,0)));

        ViewPort view3 = renderManager.createMainView("Top Left", cam3);
        view3.setClearFlags(true, true, true);
        view3.attachScene(rootNode);
        view3.setBackgroundColor(ColorRGBA.Red);

        // Setup fourth view
        Camera cam4 = cam.clone();
        cam4.setViewPort(0.6f, 1f, .5f, 1f);
        cam4.setLocation(new Vector3f(15f, 0f, 0f));
        cam4.setRotation(new Quaternion().fromAngleAxis(-FastMath.HALF_PI, new Vector3f(0,1,0)));

        ViewPort view4 = renderManager.createMainView("Top Right", cam4);
        view4.setClearFlags(true, true, true);
        view4.attachScene(rootNode);
        view4.setBackgroundColor(ColorRGBA.Yellow);

        //test multiview for gui 
        guiViewPort.getCamera().setViewPort(.5f, 1f, .5f, 1f);

        // Setup second gui view
        Camera guiCam2 = guiViewPort.getCamera().clone();
        guiCam2.setViewPort(0f, 0.5f, 0f, 0.5f);
        ViewPort guiViewPort2 = renderManager.createPostView("Gui 2", guiCam2);
        guiViewPort2.setClearFlags(false, false, false);
        guiViewPort2.attachScene(guiViewPort.getScenes().get(0));
    
    /** Add InputManager action: Left click triggers shooting. */
    inputManager.addMapping("shoot", new KeyTrigger(KeyInput.KEY_SPACE));
    inputManager.addMapping("Left", new KeyTrigger(KeyInput.KEY_LEFT));
    inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_RIGHT));
    inputManager.addMapping("Up", new KeyTrigger(KeyInput.KEY_UP));
    inputManager.addMapping("Down", new KeyTrigger(KeyInput.KEY_DOWN));
    inputManager.addMapping("PageUp", new KeyTrigger(KeyInput.KEY_PGUP));
    inputManager.addMapping("PageDown", new KeyTrigger(KeyInput.KEY_PGDN));
    inputManager.addMapping("Stationary", new KeyTrigger(KeyInput.KEY_I));
    inputManager.addMapping("Forward", new KeyTrigger(KeyInput.KEY_S));
    inputManager.addMapping("Circle", new KeyTrigger(KeyInput.KEY_T));
    inputManager.addListener(actionListener, "shoot");
    inputManager.addListener(actionListener, "Left");
    inputManager.addListener(actionListener, "Right");
    inputManager.addListener(actionListener, "Up");
    inputManager.addListener(actionListener, "Down");
    inputManager.addListener(actionListener, "PageUp");
    inputManager.addListener(actionListener, "PageDown");
    inputManager.addListener(actionListener, "Forward", "Stationary", "Circle");
    /** Initialize the scene, materials, and physics space */
    
    initMaterials();
    initTray();
    initFloor();
    initAudio();
    getPhysicsSpace().addCollisionListener(this);
    
    /** Initialize the blub */
    player = (Node) assetManager.loadModel("Models/blub/blub_quadrangulated.mesh.xml");   
    player.setLocalScale(1f);
    player.setLocalTranslation(-1f, 0.0f, 0.0f);
    rootNode.attachChild(player);
    control = player.getControl(AnimControl.class);
    control.addListener(this);
    channel = control.createChannel();
    channel.setAnim("Stationary");
    
    CapsuleCollisionShape capsuleShape = new CapsuleCollisionShape(0.335f, 1.2f, 2);
    player_phy = new RigidBodyControl(capsuleShape, 1f);
    player.addControl(player_phy);
    bulletAppState.getPhysicsSpace().add(player_phy);
    player.getControl(RigidBodyControl.class).setGravity(Vector3f.ZERO);
  }

  /**
   * Every time the shoot action is triggered, a new cannon ball is produced.
   * The ball is set up to fly from the camera position in the camera direction.
   */
  private ActionListener actionListener = new ActionListener() {
    public void onAction(String name, boolean keyPressed, float tpf) {
      if (name.equals("shoot") && !keyPressed) {
        makeCannonBall();
      }
      if (name.equals("Left")) {
        left = keyPressed;
        System.out.println(left);
        player_phy.setLinearVelocity(new Vector3f(-1.0f, 0.0f, 0.0f).mult(0.88f));
      } else if (name.equals("Right")) {
        right= keyPressed;
        player_phy.setLinearVelocity(new Vector3f(1.0f, 0.0f, 0.0f).mult(0.88f));
      } else if (name.equals("Up")) {
        up = keyPressed;
        player_phy.setLinearVelocity(new Vector3f(0.0f, 0.0f, -1.0f).mult(0.88f));
      } else if (name.equals("Down")) {
        down = keyPressed;
        player_phy.setLinearVelocity(new Vector3f(0.0f, 0.0f, 1.0f).mult(0.88f));
      } else if (name.equals("PageUp")) {
        pageup = keyPressed;
        player_phy.setLinearVelocity(new Vector3f(0.0f, 1.0f, 0.0f).mult(0.88f));
      } else if (name.equals("PageDown")) {
        pagedown = keyPressed;
        player_phy.setLinearVelocity(new Vector3f(0.0f, -1.0f, 0.0f).mult(2f));
      }
      if (name.equals("Forward") && !keyPressed) {
          Forward = !Forward;
          Stationary = false;
          Circle = false;
        if (Forward) { 
            if (!channel.getAnimationName().equals("Forward")) {
              player_phy.setLinearVelocity(Vector3f.ZERO);
              player_phy.setAngularVelocity(Vector3f.ZERO);
              player_phy.setLinearVelocity(player_phy.getPhysicsRotation().mult(new Vector3f(0.0f, 0.0f, 1.0f).mult(1.4f)));
              channel.setAnim("Forward", 0.50f);
              channel.setLoopMode(LoopMode.Loop);   
            }
        } else {
            channel.setAnim("Stationary", 0.50f);
            channel.setLoopMode(LoopMode.Loop);
            player_phy.setLinearVelocity(Vector3f.ZERO);
            player_phy.setAngularVelocity(Vector3f.ZERO);
        }
      }
      if (name.equals("Stationary") && !keyPressed) {
          Forward = false;
        if (!channel.getAnimationName().equals("Stationary")) {
          channel.setAnim("Stationary", 0.50f);
          channel.setLoopMode(LoopMode.Loop);   
        } 
      }
      if (name.equals("Circle") && !keyPressed) {
          Forward = false;
        if (!channel.getAnimationName().equals("Circle")) {
          channel.setAnim("Circle", 0.50f);
          channel.setLoopMode(LoopMode.DontLoop);
          
        }
      }
    }
  };

   private void initAudio() {
    /* gun shot sound is to be triggered by a mouse click. */
    audio_gun = new AudioNode(assetManager, "Sound/Effects/Gun.wav", DataType.Buffer);
    audio_gun.setPositional(false);
    audio_gun.setLooping(false);
    audio_gun.setVolume(2);
    rootNode.attachChild(audio_gun);

    /* nature sound - keeps playing in a loop. */
    audio_nature = new AudioNode(assetManager, "Sound/Environment/Ocean Waves.ogg", DataType.Stream);
    audio_nature.setLooping(true);  // activate continuous playing
    audio_nature.setPositional(true);
    audio_nature.setVolume(3);
    rootNode.attachChild(audio_nature);
    audio_nature.play(); // play continuously!
  }
  
  /** Initialize the materials used in this scene. */
  public void initMaterials() {
    wall_mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
    TextureKey key = new TextureKey("Textures/Terrain/BrickWall/BrickWall.jpg");
    key.setGenerateMips(true);
    Texture tex = assetManager.loadTexture(key);
    wall_mat.setTexture("ColorMap", tex);

    stone_mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
    TextureKey key2 = new TextureKey("Textures/Terrain/Rock/Rock.PNG");
    key2.setGenerateMips(true);
    Texture tex2 = assetManager.loadTexture(key2);
    stone_mat.setTexture("ColorMap", tex2);

    floor_mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
    floor_mat.setColor("Color", new ColorRGBA( 0.7f, 0.7f, 0.7f, 0.5f)); // silver'ish
    floor_mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
  }

  /** Make a solid floor and add it to the scene. */
  public void initFloor() {
    Geometry floor_geo = new Geometry("Floor", floor);
    floor_geo.setMaterial(floor_mat);
    floor_geo.setQueueBucket(RenderQueue.Bucket.Transparent);
    floor_geo.setLocalTranslation(0.0f, 0.0f, 0.0f);
    this.rootNode.attachChild(floor_geo);
    /* Make the floor physical with mass 0.0f! */
    CollisionShape floorShape =
            CollisionShapeFactory.createMeshShape(floor_geo);
    floor_phy = new RigidBodyControl(floorShape,0.0f);
    floor_geo.addControl(floor_phy);
    bulletAppState.getPhysicsSpace().add(floor_phy);
  }

  /** This loop builds a wall out of individual bricks. */
  public void initTray() {
    float horizon [] = new float [] {-2.2f, 2.2f, 0.0f};
    float height [] = new float [] {-2.0f, -0.5f, 2.0f};
    for(int x = 0; x < 3; x++) {
        RigidBodyControl jointNodeA = makeBrick(new Vector3f(horizon[x], height[x], 0.0f));
        RigidBodyControl jointNodeB = makeBrick_side(new Vector3f(horizon[x] + brickLength - 0.05f, height[x] + 0.58f, 0.0f));
        makeBrick_side(new Vector3f(horizon[x] - brickLength + 0.05f, height[x] + 0.58f, 0.0f));
        makeBrick_front(new Vector3f(horizon[x], height[x] + 0.58f, brickWidth - 0.05f));
        makeBrick_front(new Vector3f(horizon[x], height[x] + 0.58f, -brickWidth + 0.05f));
        
        jointArray[x] = new HingeJoint(jointNodeA, // A
                     jointNodeB, // B   
                     new Vector3f(brickLength-0.05f, 0.0f, 0.0f),  // pivot point local to A
                     new Vector3f(0.0f, -0.3f, 0.0f),  // pivot point local to B
                     Vector3f.UNIT_Z,           // DoF Axis of A (Z axis)
                     Vector3f.UNIT_Z  );        // DoF Axis of B (Z axis)
    bulletAppState.getPhysicsSpace().add(jointArray[x]);
    jointArray[x].enableMotor(true, 1, 2.5f);
    }
  }

  /** This method creates one individual physical brick. */
  public RigidBodyControl makeBrick(Vector3f loc) {
    /** Create a brick geometry and attach to scene graph. */
    Geometry brick_geo = new Geometry("brick", box);
    brick_geo.setMaterial(floor_mat);
    brick_geo.setQueueBucket(RenderQueue.Bucket.Transparent);
    rootNode.attachChild(brick_geo);
    /** Position the brick geometry  */
    brick_geo.setLocalTranslation(loc);
    /** Make brick physical with a mass > 0.0f. */
    brick_phy = new RigidBodyControl(0.1f);
    /** Add physical brick to physics space. */
    brick_geo.addControl(brick_phy);
    bulletAppState.getPhysicsSpace().add(brick_phy);
    return brick_phy;
  }

  public RigidBodyControl makeBrick_side(Vector3f loc) {
    /** Create a brick geometry and attach to scene graph. */
    Geometry brick_geo = new Geometry("brick", new Box(0.1f,0.6f,brickWidth));
    brick_geo.setMaterial(floor_mat);
    brick_geo.setQueueBucket(RenderQueue.Bucket.Transparent);
    rootNode.attachChild(brick_geo);
    /** Position the brick geometry  */
    brick_geo.setLocalTranslation(loc);
    /** Make brick physical with a mass > 0.0f. */
    brick_phy = new RigidBodyControl(0.0f);
    /** Add physical brick to physics space. */
    brick_geo.addControl(brick_phy);
    bulletAppState.getPhysicsSpace().add(brick_phy);
    return brick_phy;
  }
  
  public void makeBrick_front(Vector3f loc) {
    /** Create a brick geometry and attach to scene graph. */
    Geometry brick_geo = new Geometry("brick", new Box(brickLength,0.6f,0.1f));
    brick_geo.setMaterial(floor_mat);
    brick_geo.setQueueBucket(RenderQueue.Bucket.Transparent);
    rootNode.attachChild(brick_geo);
    /** Position the brick geometry  */
    brick_geo.setLocalTranslation(loc);
    /** Make brick physical with a mass > 0.0f. */
    brick_phy = new RigidBodyControl(0.0f);
    /** Add physical brick to physics space. */
    brick_geo.addControl(brick_phy);
    bulletAppState.getPhysicsSpace().add(brick_phy);
  }
  
  /** This method creates one individual physical cannon ball.
   * By defaul, the ball is accelerated and flies
   * from the camera position in the camera direction.*/
   public void makeCannonBall() {
    /** Create a cannon ball geometry and attach to scene graph. */
    Geometry ball_geo = new Geometry("cannon ball", sphere);
    ball_geo.setMaterial(stone_mat);
    rootNode.attachChild(ball_geo);
    /** Position the cannon ball  */
    ball_geo.setLocalTranslation(new Vector3f((float) ((Math.random()-0.5)*9f), 4.0f, (float) ((Math.random()-0.5)*9f)));
    /** Make the ball physcial with a mass > 0.0f */
    ball_phy = new RigidBodyControl(1f);
    /** Add physical ball to physics space. */
    ball_geo.addControl(ball_phy);
    bulletAppState.getPhysicsSpace().add(ball_phy);
    /** Accelerate the physcial ball to shoot it. */
    ball_phy.setLinearVelocity(new Vector3f((float)(Math.random()-0.5f)*2, -(float)Math.random(), (float)(Math.random()-0.5f)*2).mult((float)Math.random()*10f));
  }
   
    public void makeDeathBall(Vector3f loc) {
    /** Create a cannon ball geometry and attach to scene graph. */
    Geometry ball_geo = new Geometry("Death ball", sphere);
    Material mat = new Material(assetManager,
          "Common/MatDefs/Misc/Unshaded.j3md");
    mat.setColor("Color", ColorRGBA.Red);
    ball_geo.setMaterial(mat);
    rootNode.attachChild(ball_geo);
    /** Position the cannon ball  */
    ball_geo.setLocalTranslation(loc);
    /** Make the ball physcial with a mass > 0.0f */
    ball_phy = new RigidBodyControl(1f);
    /** Add physical ball to physics space. */
    ball_geo.addControl(ball_phy);
    bulletAppState.getPhysicsSpace().add(ball_phy);
    /** Accelerate the physcial ball to shoot it. */
    ball_phy.setLinearVelocity(new Vector3f((float)(Math.random()-0.5f)*2, (float)(Math.random()-0.3)*2, (float)(Math.random()-0.5f)*2).mult(10f));
  }

    @Override
    public void collision(PhysicsCollisionEvent event) {
        if ("blub_quadrangulated-ogremesh".equals(event.getNodeA().getName()) || "blub_quadrangulated-ogremesh".equals(event.getNodeB().getName())) {
            if ("cannon ball".equals(event.getNodeA().getName()) || "cannon ball".equals(event.getNodeB().getName())) {
                Vector3f v = player.getLocalTranslation();
                bulletAppState.getPhysicsSpace().remove(player_phy);
                rootNode.detachChild(player);
                for(int x = 0; x < 20; x ++) {
                    makeDeathBall(v);
                }
                audio_gun.playInstance();
            }
        }
        if ("blub_quadrangulated-ogremesh".equals(event.getNodeA().getName()) || "blub_quadrangulated-ogremesh".equals(event.getNodeB().getName())) {
            if ("brick".equals(event.getNodeA().getName()) || "brick".equals(event.getNodeB().getName()) || 
                    "Floor".equals(event.getNodeA().getName()) || "Floor".equals(event.getNodeB().getName())) {
                Quaternion pitch90 = new Quaternion();
                pitch90.fromAngleAxis((float) ((FastMath.PI/2)*(1+0.8*Math.random())), new Vector3f(0,1,0));
                player_phy.setPhysicsRotation(pitch90);
                player_phy.setLinearVelocity(Vector3f.ZERO);
                player_phy.setAngularVelocity(Vector3f.ZERO);
                player_phy.setLinearVelocity(pitch90.mult(new Vector3f(0,0,1).mult(1.4f)));
                forFlag++;
            }
        }
    }
    
    public void onAnimCycleDone(AnimControl control, AnimChannel channel, String animName) {
    if (animName.equals("Circle")) {
      channel.setAnim("Forward", 0.50f);
      channel.setLoopMode(LoopMode.Loop);
      channel.setSpeed(1f);
    }
    if (animName.equals("Forward")) {
      channel.setAnim("Forward", 0.50f);
      channel.setLoopMode(LoopMode.Loop);
      channel.setSpeed(1f);
    }
    if (animName.equals("Stationary")) {
      channel.setAnim("Stationary", 0.50f);
      channel.setLoopMode(LoopMode.Loop);
      channel.setSpeed(1f);
      player_phy.setLinearVelocity(Vector3f.ZERO);
      player_phy.setAngularVelocity(Vector3f.ZERO);
    }
  }

  public void onAnimChange(AnimControl control, AnimChannel channel, String animName) {
    // unused
  }
  
  public void simpleUpdate(float tpf) {
      if(player_phy.getLinearVelocity().x < 1.4f & player_phy.getLinearVelocity().x > 1.2f || 
              player_phy.getLinearVelocity().y < 1.4f & player_phy.getLinearVelocity().y > 1.2f || 
              player_phy.getLinearVelocity().z < 1.4f & player_phy.getLinearVelocity().z > 1.2f){
          Quaternion pitch90 = new Quaternion();
          pitch90.fromAngleAxis((float) ((FastMath.PI/2)*forFlag*(2*(Math.random()-0.5))), new Vector3f(0,1,0));
          player_phy.setPhysicsRotation(pitch90);
          player_phy.setLinearVelocity(Vector3f.ZERO);
          player_phy.setAngularVelocity(Vector3f.ZERO);
          player_phy.setLinearVelocity(pitch90.mult(new Vector3f(0,0,1).mult(1.5f)));
//          player_phy.setLinearVelocity((player_phy.getPhysicsRotation()).mult(new Vector3f(0,0,1).mult(1.5f)));
      }
  }
}
