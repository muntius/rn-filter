
package com.firenoid.rnfilter;

import android.support.annotation.Nullable;
import android.util.Log;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.ViewProps;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.views.image.ImageResizeMode;
import com.facebook.react.views.image.ReactImageView;

import java.util.Map;

public class RNFilterManager extends SimpleViewManager<RNFilterView> {

  public static final String REACT_CLASS = "RNFilter";
  public static final int COMMAND_CAPTURE_CURRENT_VIEW = 1;
  public static final int COMMAND_SET_BRIT = 2;
  public static final int COMMAND_SET_CONTR = 3;
  public static final int COMMAND_SET_SATUR = 4;
  public static final int COMMAND_SET_FILTER = 5;
  public static final int COMMAND_GEN_FILTERS = 6;


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
    Log.d("React"," View manager createViewInstance:");
    return new RNFilterView(reactContext, mContextModule.getActivity());

  }

  @ReactProp(name = "src")
  public void setSrc(RNFilterView view, @Nullable String sources) {
//    Log.d("React:", "source before");
//    Log.d("React:", String.valueOf( sources ));
    view.setSource(sources);
  }


  @ReactProp(name = "filter")
  public void filter(RNFilterView view, @Nullable double id) {
    view.setFilter(id);
  }


  @ReactProp(name = "thumbnail")
  public void filter(RNFilterView view, @Nullable boolean value) {
    view.setThumbnail(value);
  }

//  @ReactProp(name = "contrast")
//  public void setContrast(RNFilterView view, @Nullable float progress) {
//    view.setContrast(progress);
//  }

//  @ReactProp(name = "brightness")
//  public void setBrightness(RNFilterView view, @Nullable float progress) {
//    view.setBrightness(progress);
//  }


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
    return MapBuilder.of(
            "capture",
            COMMAND_CAPTURE_CURRENT_VIEW,
            "setSaturation",
            COMMAND_SET_SATUR,
            "setContrast",
            COMMAND_SET_CONTR,
            "setBrightness",
            COMMAND_SET_BRIT,
            "setFilter",
            COMMAND_SET_FILTER,
            "generateFilters",
            COMMAND_GEN_FILTERS
    );
  }
  @Override
  public void receiveCommand(final RNFilterView root, int commandId, @javax.annotation.Nullable ReadableArray args) {
    // This will be called whenever a command is sent from react-native.
    switch (commandId) {
      case COMMAND_CAPTURE_CURRENT_VIEW:
//                root.snapShot = true;
//                root.widthImage = args.getInt(1);
//                root.heightImage = args.getInt(0);
        break;
//                root.requestRender();
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
      case COMMAND_SET_FILTER:
        root.setFilter(args.getDouble(0));
        break;
      case COMMAND_GEN_FILTERS:
//        root.generateFilters();
        break;
    }
  }


}