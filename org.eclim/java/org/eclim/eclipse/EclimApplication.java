/**
 * Copyright (C) 2005 - 2012  Eric Van Dewoestine
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.eclim.eclipse;

import java.util.Map;

import org.eclim.logging.Logger;

import org.eclipse.e4.ui.internal.workbench.E4Workbench;

import org.eclipse.equinox.app.IApplicationContext;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

import org.eclipse.ui.internal.ide.application.IDEApplication;

/**
 * Application used to start a headless version of eclipse running the eclim
 * daemon.
 *
 * @author Eric Van Dewoestine
 */
public class EclimApplication
  extends IDEApplication
{
  private static final Logger logger = Logger.getLogger(EclimApplication.class);

  private static EclimApplication instance;
  private static boolean stopping;

  @SuppressWarnings({"rawtypes", "unchecked"})
  @Override
  public Object start(IApplicationContext context)
    throws Exception
  {
    Map args = context.getArguments();

    String[] appArgs = (String[])args.get(IApplicationContext.APPLICATION_ARGS);
    String[] presentationArgs = new String[]{
      // prevent headless worbench settings from being persisted to
      // workbench.xmi, which can break the default workbench.
      "-" + E4Workbench.PERSIST_STATE,
      "false",
      // inject our headless presentation engine.
      "-" + E4Workbench.PRESENTATION_URI_ARG,
      "bundleclass://org.eclim/org.eclim.eclipse.e4.HeadlessPresentationEngine",
    };
    String[] newArgs = new String[appArgs.length + presentationArgs.length];
    System.arraycopy(appArgs, 0, newArgs, 0, appArgs.length);
    System.arraycopy(
        presentationArgs, 0, newArgs, appArgs.length, presentationArgs.length);

    args.put(IApplicationContext.APPLICATION_ARGS, newArgs);

    instance = this;

    Runtime.getRuntime().addShutdownHook(new Thread(){
      public void run(){
        try{
          EclimApplication.shutdown();
        }catch(Exception ex) {
          logger.error("Error durring shutdown.", ex);
        }
      }
    });

    // Note: starting of EclimDaemon is handled by EclimStartup so that the
    // daemon is started after the workbench is available.

    return super.start(context);
  }

  public static boolean isEnabled()
  {
    return instance != null;
  }

  public static synchronized void shutdown()
    throws Exception
  {
    if (instance != null && !stopping){
      stopping = true;

      EclimDaemon.getInstance().stop();

      final IWorkbench workbench = PlatformUI.getWorkbench();
      workbench.getDisplay().syncExec(new Runnable(){
        public void run() {
          workbench.getActiveWorkbenchWindow().close();
        }
      });
    }
  }
}
