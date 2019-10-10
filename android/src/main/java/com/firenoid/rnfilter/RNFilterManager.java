
package com.firenoid.rnfilter;

import android.support.annotation.Nullable;
import android.util.Log;

import com.facebook.common.internal.ImmutableMap;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;

import java.util.HashMap;
import java.util.Map;

public class RNFilterManager extends SimpleViewManager<RNFilterView> {

  public static final String REACT_CLASS = "RNFilter";
  public final int COMMAND_CAPTURE_CURRENT_VIEW = 1;
  public final int COMMAND_SET_BRIT = 2;
  public final int COMMAND_SET_CONTR = 3;
  public final int COMMAND_SET_SATUR = 4;
  public final int COMMAND_SET_BLUR = 5;
  public final int COMMAND_SET_VIGN = 6;
  public final int COMMAND_SET_RESET = 7;
  public final int COMMAND_SET_ORIG = 9;



  private RNFilterContextModule mContextModule;

  public RNFilterManager(ReactApplicationContext reactContext) {
    mContextModule = new RNFilterContextModule(reactContext);
  }

  @Override
  public String getName() {
    return REACT_CLASS;
  }


  @Override
  protected RNFilterView createViewInstance(ThemedReactContext reactContext) {
    return new RNFilterView(reactContext, mContextModule.getActivity());

  }

  @ReactProp(name = "src")
  public void setSrc(RNFilterView view, @Nullable String sources) {
    view.setSource(sources);
  }


  @ReactProp(name = "thumbnail")
  public void filter(RNFilterView view, @Nullable boolean value) {
    view.setThumbnail(value);
  }


  @Override
  public @javax.annotation.Nullable
  Map getExportedCustomDirectEventTypeConstants() {
    return MapBuilder.of(
//            "surfaceCreate",
//            MapBuilder.of("registrationName", "onSurfaceCreate"),
            "dataReturned",
            MapBuilder.of("registrationName", "onDataReturned"),
            "thumbsReturned",
            MapBuilder.of("registrationName", "onThumbsReturned")
    );
  }



  @javax.annotation.Nullable
  @Override
  public Map<String, Integer> getCommandsMap() {
    // You need to implement this method and return a map with the readable
    // name and constant for each of your commands. The name you specify
    // here is what you'll later use to access it in react-native.
    Map<String,Integer > map = new HashMap<String,Integer >();
    map.put("capture", COMMAND_CAPTURE_CURRENT_VIEW);
    map.put("setSaturation", COMMAND_SET_SATUR);
    map.put("setContrast", COMMAND_SET_CONTR);
    map.put("setBrightness", COMMAND_SET_BRIT);
    map.put("setBlur", COMMAND_SET_BLUR);
    map.put("setVignette", COMMAND_SET_VIGN);
    map.put("setOriginal", COMMAND_SET_ORIG);
    map.put("setReset", COMMAND_SET_RESET);
    ImmutableMap immutableMap = ImmutableMap.copyOf(map);
    return immutableMap;
  }
  @Override
  public void receiveCommand(final RNFilterView root, int commandId, @javax.annotation.Nullable ReadableArray args) {
    // This will be called whenever a command is sent from react-native.
    switch (commandId) {
      case COMMAND_CAPTURE_CURRENT_VIEW:
        root.SaveImage = true;
        root.widthImage = args.getInt(1);
        root.heightImage = args.getInt(0);
        root.requestRender();
        break;
//
      case COMMAND_SET_BRIT:
        Log.d("Brit",String.valueOf(args));
        root.setBrightness(args.getDouble(0));

        break;
      case COMMAND_SET_CONTR:
        root.setContrast(args.getDouble(0));

        break;
      case COMMAND_SET_SATUR:
        root.setSaturation(args.getDouble(0));
        break;
      case COMMAND_SET_BLUR:
        root.setBlur(args.getDouble(0));
        break;
      case COMMAND_SET_VIGN:
        root.setVignette(args.getDouble(0));
        break;
      case COMMAND_SET_ORIG:
        root.setOriginal(args.getBoolean(0));
        break;
        case COMMAND_SET_RESET:
        root.setReset();
        break;
    }
  }


}