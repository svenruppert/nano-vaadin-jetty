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

import com.vaadin.browserless.BaseBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonTester;
import com.vaadin.flow.component.html.Span;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ViewPackages(classes = DemoView.class)
class DemoViewBrowserlessTest extends BaseBrowserlessTest {

  @BeforeEach
  void setUp() {
    initVaadinEnvironment();
  }

  @AfterEach
  void tearDown() {
    cleanVaadinEnvironment();
  }

  @Test
  void buttonClickUpdatesCounter() {
    navigate(DemoView.class);

    Button button = $(Button.class).withId(DemoView.CLICK_BUTTON_ID).single();
    Span count = $(Span.class).withId(DemoView.CLICK_COUNT_ID).single();

    assertEquals("0", count.getText());

    test(ButtonTester.class, button).click();
    assertEquals("1", count.getText());

    test(ButtonTester.class, button).click();
    assertEquals("2", count.getText());
  }

  @Override
  protected String testingEngine() {
    return "JUnit 6";
  }
}
