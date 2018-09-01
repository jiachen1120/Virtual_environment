package blub;

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.AnimEventListener;
import com.jme3.animation.LoopMode;
import com.jme3.app.SimpleApplication;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;

/** Sample 7 - how to load an OgreXML model and play an animation,
 * using channels, a controller, and an AnimEventListener. */
public class main extends SimpleApplication
  implements AnimEventListener {
  private AnimChannel channel;
  private AnimControl control;
  private Vector3f camDir = new Vector3f();
  private Vector3f camLeft = new Vector3f();
  Node player;
  public static void main(String[] args) {
    main app = new main();
    app.start();
  }

  @Override
  public void simpleInitApp() {
    //camDir.set(cam.getDirection()).multLocal(0.6f);
    //camLeft.set(cam.getLeft()).multLocal(0.4f);
    cam.setLocation(new Vector3f(0,5,20));
    viewPort.setBackgroundColor(ColorRGBA.LightGray);
    initKeys();
    DirectionalLight dl = new DirectionalLight();
    dl.setDirection(new Vector3f(-0.1f, -1f, -1).normalizeLocal());
    rootNode.addLight(dl);
    player = (Node) assetManager.loadModel("Models/blub/blub_quadrangulated.mesh.xml");   
    player.setLocalScale(2f);
    rootNode.attachChild(player);
    control = player.getControl(AnimControl.class);
    control.addListener(this);
    channel = control.createChannel();
    channel.setAnim("Stationary");
  }

  public void onAnimCycleDone(AnimControl control, AnimChannel channel, String animName) {
    if (animName.equals("Circle")) {
      channel.setAnim("Stationary", 0.50f);
      channel.setLoopMode(LoopMode.DontLoop);
      channel.setSpeed(1f);
    }
//    if (animName.equals("Forward")) {
//      channel.setAnim("Stationary", 0.50f);
//      channel.setLoopMode(LoopMode.DontLoop);
//      channel.setSpeed(1f);
//    }
  }

  public void onAnimChange(AnimControl control, AnimChannel channel, String animName) {
    // unused
  }

  /** Custom Keybinding: Map named actions to inputs. */
  private void initKeys() {
    inputManager.addMapping("Stationary", new KeyTrigger(KeyInput.KEY_I));
    inputManager.addMapping("Forward", new KeyTrigger(KeyInput.KEY_S));
    inputManager.addMapping("Circle", new KeyTrigger(KeyInput.KEY_T));
    inputManager.addListener(actionListener, "Forward", "Stationary", "Circle");
  }
  private ActionListener actionListener = new ActionListener() {
    public void onAction(String name, boolean keyPressed, float tpf) {
      if (name.equals("Forward") && !keyPressed) {
        if (!channel.getAnimationName().equals("Forward")) {
          channel.setAnim("Forward", 0.50f);
          channel.setLoopMode(LoopMode.Loop);   
        }
      }
      if (name.equals("Stationary") && !keyPressed) {
        if (!channel.getAnimationName().equals("Stationary")) {
          channel.setAnim("Stationary", 0.50f);
          channel.setLoopMode(LoopMode.Loop);   
        }
      }
      if (name.equals("Circle") && !keyPressed) {
        if (!channel.getAnimationName().equals("Circle")) {
          channel.setAnim("Circle", 0.50f);
          channel.setLoopMode(LoopMode.DontLoop);
        }
      }
    }
  };
}