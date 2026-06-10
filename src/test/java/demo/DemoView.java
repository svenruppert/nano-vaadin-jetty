package demo;

/*-
 * #%L
 * nano-vaadin-jetty
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2017 - 2026 Vaadin
 * %%
 * Licensed under the EUPL, Version 1.2 or – as soon they will be
 * approved by the European Commission - subsequent versions of the
 * EUPL (the "Licence");
 *
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 * #L%
 */

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route("")
public class DemoView extends Composite<Div> {

  public static final String CLICK_BUTTON_ID = "demo-click-button";
  public static final String CLICK_COUNT_ID = "demo-click-count";

  private int clickCount;

  public DemoView() {
    Button button = new Button("Click");
    button.setId(CLICK_BUTTON_ID);

    Span count = new Span("0");
    count.setId(CLICK_COUNT_ID);

    button.addClickListener(event -> count.setText(Integer.toString(++clickCount)));

    getContent().add(new VerticalLayout(
        new H2("Nano Vaadin Jetty Test Demo"),
        button,
        count
    ));
  }
}
