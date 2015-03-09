package com.example.hermanmak.doomhack;

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.AnimEventListener;
import com.jme3.animation.LoopMode;
import com.jme3.app.SimpleApplication;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;

public class SensorAccessJME extends SimpleApplication implements AnimEventListener {

    private static final String TAG = "SensorAccessJME";
    // The geometry which will represent the video background
    private Geometry mVideoBGGeom;
    private Geometry mVideoBGGeom2;
    // The material which will be applied to the video background geometry.
    private Material mvideoBGMat;
    private Material mvideoBGMat2;
    // The texture displaying the Android camera preview frames.
    private Texture2D mCameraTexture;
    private Texture2D mCameraTexture2;
    // the JME image which serves as intermediate storage place for the Android
    // camera frame before the pixels get uploaded into the texture.
    private Image mCameraImage;
    private Image mCameraImage2;
    // A flag indicating if the scene has been already initialized.
    private boolean mSceneInitialized = false;
    private boolean mSceneInitialized2 = false;
    // A flag indicating if the JME Image has been already initialized.
    private boolean mVideoImageInitialized = false;
    // A flag indicating if a new Android camera image is available.
    boolean mNewCameraFrameAvailable = false;
    boolean mNewCameraFrameAvailable2 = false;


    // An intermediate Quaternion used to hold the converted sensor orients
    private Quaternion mRotXYZQ;
    // The initial camera orientation
    private Quaternion mInitialCamRotation;
    // The final camera rotation
    private Quaternion mCurrentCamRotation;

    //A flag indicating if a new Rotation is available
    private boolean mNewCamRotationAvailable =false;

    //A flag indicating if a new Rotation is available
    private boolean mNewUserRotationFusedAvailable =false;

    // our virtual camera for rendering the 3D scene
    Camera fgCam;

    private float mForegroundCamFOVY = 50; // for a Samsung Galaxy SII

    // for animation
    // The controller allows access to the animation sequences of the model
    private AnimControl mAniControl;
    // the channel is used to run one animation sequence at a time
    private AnimChannel mAniChannel;

    public static void main(String[] args) {
        SensorAccessJME app = new SensorAccessJME();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        // Do not display statistics
        setDisplayStatView(false);
        setDisplayFps(false);
        // we use our custom viewports - so the main viewport does not need the  rootNode
        viewPort.detachScene(rootNode);
        initVideoBackground(settings.getWidth(), settings.getHeight());
        initForegroundScene();

        initBackgroundCamera();
        initBackgroundCamera2();
        initForegroundCamera(mForegroundCamFOVY);

        mSceneInitialized = true;
    }

    // This function creates the geometry, the viewport and the virtual camera
    // needed for rendering the incoming Android camera frames in the scene
    // graph
    public void initVideoBackground(int screenWidth, int screenHeight) {
        // Create a Quad shape.
        Quad videoBGQuad = new Quad(1, 1, true);
        // Create a Geometry with the Quad shape
        mVideoBGGeom = new Geometry("quad", videoBGQuad);
        float newWidth = 1.f * screenWidth / screenHeight;
        // Center the Geometry in the middle of the screen.
        mVideoBGGeom.setLocalTranslation(0.f * newWidth, -0.35f, 0.f);//
        // Scale (stretch) the width of the Geometry to cover the whole screen
        // width.
        mVideoBGGeom.setLocalScale(0.5f * newWidth, 0.7f, 1);
        // Apply a unshaded material which we will use for texturing.
        mvideoBGMat = new Material(assetManager,
                "Common/MatDefs/Misc/Unshaded.j3md");
        mVideoBGGeom.setMaterial(mvideoBGMat);
        // Create a new texture which will hold the Android camera preview frame
        // pixels.
        mCameraTexture = new Texture2D();

        mVideoBGGeom2 = new Geometry("quad2", videoBGQuad);
        mVideoBGGeom2.setLocalTranslation(-0.5f * newWidth, -0.35f, 0.f);
        mVideoBGGeom2.setLocalScale(0.5f * newWidth, 0.7f, 1);
        mvideoBGMat2 = new Material(assetManager,
                "Common/MatDefs/Misc/Unshaded.j3md");
        mVideoBGGeom2.setMaterial(mvideoBGMat2);
        mCameraTexture2 = new Texture2D();
        mSceneInitialized2 = true;

    }

    public void initBackgroundCamera() {
        // Create a custom virtual camera with orthographic projection
        Camera videoBGCam = cam.clone();
        videoBGCam.setParallelProjection(true);
        // Also create a custom viewport.
        ViewPort videoBGVP = renderManager.createMainView("VideoBGView",
                videoBGCam);
        // Attach the geometry representing the video background to the
        // viewport.
        videoBGVP.attachScene(mVideoBGGeom);
    }

    public void initBackgroundCamera2() {
        // Create a custom virtual camera with orthographic projection
        Camera videoBGCam2 = cam.clone();
        videoBGCam2.setParallelProjection(true);
        // Also create a custom viewport.
        ViewPort videoBGVP2 = renderManager.createMainView("VideoBGView2",
                videoBGCam2);
        // Attach the geometry representing the video background to the
        // viewport.
        videoBGVP2.attachScene(mVideoBGGeom2);
    }

    public void initForegroundScene() {
        // Load a model from test_data (OgreXML + material + texture)
        Spatial monster = assetManager.loadModel("Models/monster/monster.mesh.xml");
        monster.scale(0.05f, 0.05f, 0.05f);
        monster.rotate(1.8f, -3.0f, 0.0f);
        monster.setLocalTranslation(0.0f, -1f, -5.0f);

        rootNode.attachChild(monster);

        // You must add a light to make the model visible
        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(new Vector3f(-0.1f, -0.7f, -1.0f));
        rootNode.addLight(sun);

        Spatial monster2 = assetManager.loadModel("Models/monster/monster.mesh.xml");
        monster2.scale(0.05f, 0.05f, 0.05f);
        monster2.setLocalTranslation(-5f, -2.f, 0.0f);
        monster2.rotate(2.2f, -2.322f, 0.0f);
        rootNode.attachChild(monster2);
        DirectionalLight sun2 = new DirectionalLight();
        sun2.setDirection(new Vector3f(-0.1f, -0.7f, -1.0f));
        rootNode.addLight(sun2);

        Spatial monster3 = assetManager.loadModel("Models/monster/monster.mesh.xml");
        monster3.scale(0.05f, 0.05f, 0.05f);
        monster3.setLocalTranslation(5f, -2.5f, -9.0f);
        monster3.rotate(2.2f, -2.322f, 0.0f);
        rootNode.attachChild(monster3);
        DirectionalLight sun6 = new DirectionalLight();
        sun6.setDirection(new Vector3f(-0.1f, -0.7f, -1.0f));
        rootNode.addLight(sun6);




        Spatial ninja = assetManager.loadModel("Models/Ninja/Ninja.mesh.xml");
        ninja.scale(0.025f, 0.025f, 0.025f);
        ninja.rotate(0.0f, -3.0f, 0.0f);
        ninja.setLocalTranslation(-8.0f, -2.5f, -23.0f);
        rootNode.attachChild(ninja);

        DirectionalLight sun3 = new DirectionalLight();
        sun3.setDirection(new Vector3f(-0.1f, -0.7f, -1.0f));
        rootNode.addLight(sun3);

        mAniControl = ninja.getControl(AnimControl.class);
        mAniControl.addListener(this);
        mAniChannel = mAniControl.createChannel();
        mAniChannel.setAnim("Walk");
        mAniChannel.setLoopMode(LoopMode.Loop);
        mAniChannel.setSpeed(1f);

        Spatial ninja2 = assetManager.loadModel("Models/Ninja/Ninja.mesh.xml");
        ninja2.scale(0.025f, 0.025f, 0.025f);
        ninja2.rotate(0.0f, -2.332f, 0.0f);
        ninja2.setLocalTranslation(-15.0f, -2f, -19.0f);
        rootNode.attachChild(ninja2);

        DirectionalLight sun4 = new DirectionalLight();
        sun4.setDirection(new Vector3f(-0.1f, -0.7f, -1.0f));
        rootNode.addLight(sun4);

        mAniControl = ninja2.getControl(AnimControl.class);
        mAniControl.addListener(this);
        mAniChannel = mAniControl.createChannel();
        mAniChannel.setAnim("Kick");
        mAniChannel.setLoopMode(LoopMode.Loop);
        mAniChannel.setSpeed(1f);

        Spatial ninja3 = assetManager.loadModel("Models/Ninja/Ninja.mesh.xml");
        ninja3.scale(0.025f, 0.025f, 0.025f);
        ninja3.rotate(0.0f, -2.332f, 0.0f);
        ninja3.setLocalTranslation(17.0f, -2f, -10.0f);
        rootNode.attachChild(ninja3);

        DirectionalLight sun5 = new DirectionalLight();
        sun5.setDirection(new Vector3f(-0.1f, -0.7f, -1.0f));
        rootNode.addLight(sun5);

        mAniControl = ninja3.getControl(AnimControl.class);
        mAniControl.addListener(this);
        mAniChannel = mAniControl.createChannel();
        mAniChannel.setAnim("Attack2");
        mAniChannel.setLoopMode(LoopMode.Loop);
        mAniChannel.setSpeed(1f);


    }

    public void initForegroundCamera(float fovY) {

        fgCam = new Camera(settings.getWidth(), settings.getHeight());
        fgCam.setLocation(new Vector3f(0f, 0f, 0f));
        fgCam.setAxes(new Vector3f(-1f,0f,0f), new Vector3f(0f,1f,0f), new Vector3f(0f,0f,-1f));
        mInitialCamRotation = new Quaternion();
        mCurrentCamRotation = new Quaternion();
        mInitialCamRotation.fromAxes(new Vector3f(-1f,0f,0f), new Vector3f(0f,1f,0f), new Vector3f(0f,0f,-1f));

        mRotXYZQ = new Quaternion();

        fgCam.setFrustumPerspective(fovY,  settings.getWidth()/settings.getHeight(), 1, 1000);
        ViewPort fgVP = renderManager.createMainView("ForegroundView", fgCam);
        fgVP.attachScene(rootNode);
        fgVP.setClearFlags(false, true, false);
        fgVP.setBackgroundColor(ColorRGBA.Blue);
    }

    public void onAnimCycleDone(AnimControl control, AnimChannel channel, String animName) {
        // unused
    }

    public void onAnimChange(AnimControl control, AnimChannel channel, String animName) {
        // unused
    }

    // This method retrieves the preview images from the Android world and puts
    // them into a JME image.
    public void setVideoBGTexture(final Image image) {
        if (!mSceneInitialized) {
            return;
        }
        mCameraImage = image;
        mCameraImage2 = image;
        mNewCameraFrameAvailable = true;
        mNewCameraFrameAvailable2 = true;
    }

    public void setRotation(Quaternion rotation ) {
        if (!mSceneInitialized) {
            return;
        }
        mCurrentCamRotation=rotation;
        mNewCamRotationAvailable =true;
    }

    public void setRotation(float pitch, float roll, float heading) {
        if (!mSceneInitialized) {
            return;
        }
//		pitch: cams x axis
// 		roll: cams y axis
//		heading: cams z axis
        mRotXYZQ.fromAngles(pitch , roll - FastMath.HALF_PI, 0);
        mCurrentCamRotation = mInitialCamRotation.mult(mRotXYZQ);
        mNewCamRotationAvailable = true;
    }

    @Override
    public void simpleUpdate(float tpf) {
        if (mNewCameraFrameAvailable) {
            mCameraTexture.setImage(mCameraImage);
            mCameraTexture2.setImage((mCameraImage2));
            mvideoBGMat.setTexture("ColorMap", mCameraTexture);
            mvideoBGMat2.setTexture("ColorMap", mCameraTexture2);
        }

        if (mNewCamRotationAvailable) {
            fgCam.setAxes(mCurrentCamRotation);
            mNewCamRotationAvailable=false;
        }

        mVideoBGGeom.updateLogicalState(tpf);
        mVideoBGGeom.updateGeometricState();
        mVideoBGGeom2.updateLogicalState(tpf);
        mVideoBGGeom2.updateGeometricState();
    }

    @Override
    public void simpleRender(RenderManager rm) {
        // unused
    }
}