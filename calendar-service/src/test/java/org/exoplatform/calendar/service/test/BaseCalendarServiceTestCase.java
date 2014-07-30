/*
 * Copyright (C) 2003-2008 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.calendar.service.test;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

import org.exoplatform.calendar.service.Calendar;
import org.exoplatform.calendar.service.CalendarService;
import org.exoplatform.calendar.service.GroupCalendarData;
import org.exoplatform.calendar.service.Utils;
import org.exoplatform.component.test.AbstractKernelTest;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.Membership;
import org.exoplatform.services.organization.MembershipHandler;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.MembershipEntry;

/**
 * Created by The eXo Platform SAS
 * @author : Hung nguyen
 *          hung.nguyen@exoplatform.com
 * May 7, 2008  
 */

@ConfiguredBy({
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.portal-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.test.jcr-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.identity-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/test-portal-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/exo.calendar.component.core.test.configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/exo.calendar.test.jcr-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/exo.calendar.test.portal-configuration.xml")
})

public abstract class BaseCalendarServiceTestCase extends AbstractKernelTest {

  protected static Log log = ExoLogger.getLogger("cs.calendar.services.test");

  protected TimeZone tz = java.util.Calendar.getInstance().getTimeZone();
  protected String timeZone = tz.getID();
  protected String   username = "root";
  protected SimpleDateFormat df = new SimpleDateFormat(Utils.DATE_TIME_FORMAT) ;

  protected OrganizationService organizationService_;
  protected CalendarService calendarService_;

  @Override
  public void setUp() throws Exception {
    begin();

    // Init services
    organizationService_ = getService(OrganizationService.class);
    calendarService_ = getService(CalendarService.class);

    // Login user
    login(username);
  }

  @Override
  public void tearDown() throws Exception {
    cleanData();
    end();
  }

  protected void cleanData() throws Exception {
    //. Get all private calendar of user
    List<Calendar> cals = calendarService_.getUserCalendars(username, true);

    //. Load all group calendar
    List<Group> groups = new ArrayList<Group>();
    groups.addAll(organizationService_.getGroupHandler().findGroupsOfUser(username));
    String[] groupIds = new String[groups.size()];
    for (int i = 0; i < groups.size(); i++) {
      groupIds[i] = groups.get(i).getId();
    }
    for (GroupCalendarData g : calendarService_.getGroupCalendars(groupIds, true, username)) {
      cals.addAll(g.getCalendars());
    }

    //. Find all shared calendar
    GroupCalendarData gData = calendarService_.getSharedCalendars(username, true);
    if (gData != null) cals.addAll(gData.getCalendars());

    //. Remove all calendar
    for (int i = 0; i < cals.size(); i++) {
      String id = cals.get(i).getId();
      calendarService_.removeUserCalendar(username, id);
      calendarService_.removePublicCalendar(id);
      calendarService_.removeSharedCalendar(username, id);
    }
  }

  protected Calendar createCalendar(String name, String description) {
    try {
      // Create and save calendar
      Calendar calendar = new Calendar();
      calendar.setName(name);
      calendar.setDescription(description);
      calendar.setPublic(false);
      calendarService_.saveUserCalendar(username, calendar, true);
      return calendar;
    } catch (Exception e) {
      fail("Exception when trying to create new calendar", e);
    }
    return null;
  }

  protected Calendar createPublicCalendar(String name, String description) {
    try {
      Calendar publicCalendar = new Calendar();
      publicCalendar.setName(name);
      publicCalendar.setDescription(description);
      publicCalendar.setPublic(true);
      publicCalendar.setGroups(new String[]{"/platform/users","/organization/management/executive-board"});
      calendarService_.savePublicCalendar(publicCalendar, true);
      return publicCalendar;
    } catch (Exception e) {
      fail("Exception while create a public calendar", e);
      return null;
    }
  }

  @SuppressWarnings("unchecked")
  protected  <T> T getService(Class<T> clazz) {
    return (T) getContainer().getComponentInstanceOfType(clazz);
  }

  protected void login(String username) {
    List<MembershipEntry> entries = new LinkedList<MembershipEntry>();

    MembershipHandler mHandler = organizationService_.getMembershipHandler();
    try {
      Collection<Membership> memberships = mHandler.findMembershipsByUser(username);
      for (Membership m : memberships) {
        entries.add(new MembershipEntry(m.getGroupId(), m.getMembershipType()));
      }
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    };
    login(username, entries);
  }

  private void login(String username, Collection<MembershipEntry> entries) {
    Identity identity = new Identity(username, entries);
    ConversationState state = new ConversationState(identity);
    ConversationState.setCurrent(state);
  }
}
