package com.appcoins.sdk.billing.payasguest;

import android.content.Context;
import android.content.pm.PackageManager;
import com.aptoide.apk.injector.extractor.data.ExtractorV2;
import com.aptoide.apk.injector.extractor.domain.IExtract;
import java.io.File;

public class OemIdExtractorFromExternalLib implements OemIdExtractor {

  private final Context context;
  private final IExtract extractor;

  public OemIdExtractorFromExternalLib(Context context) {
    this.context = context;
    this.extractor = new ExtractorV2();
  }

  @Override public String extract(String packageName) {
    String sourceDir = getApplicationDir(packageName);
    if (sourceDir == null) return null;
    return extractor.extract(new File(sourceDir));
  }

  private String getApplicationDir(String packageName) {
    try {
      return context.getPackageManager()
          .getApplicationInfo(packageName, 0).sourceDir;
    } catch (PackageManager.NameNotFoundException e) {
      e.printStackTrace();
      return null;
    }
  }
}