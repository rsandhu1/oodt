/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.apache.oodt.pcs.opsui;

//OODT imports
import org.apache.oodt.pcs.opsui.status.StatusPage;

//Wicket imports
import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.link.Link;

/**
 *
 * Adds a bread crumb link to the OPSUI app home, 
 * the PCS status page.
 *
 * @author mattmann
 * @version $Revision$
 *
 */
public class WorkflowCrumbedPage extends BasePage {

  /**
   * @param parameters
   */
  public WorkflowCrumbedPage(PageParameters parameters) {
    super(parameters);
    add(new Link("crumb_home_link"){
      /* (non-Javadoc)
       * @see org.apache.wicket.markup.html.link.Link#onClick()
       */
      @Override
      public void onClick() {
        setResponsePage(StatusPage.class);        
      }
    });
  }

}
