/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.PathNotFoundException;

import org.exoplatform.calendar.service.Attachment;
import org.exoplatform.calendar.service.Calendar;
import org.exoplatform.calendar.service.CalendarEvent;
import org.exoplatform.calendar.service.CalendarService;
import org.exoplatform.calendar.service.CalendarSetting;
import org.exoplatform.calendar.service.EventCategory;
import org.exoplatform.calendar.service.EventQuery;
import org.exoplatform.calendar.service.GroupCalendarData;
import org.exoplatform.calendar.service.Reminder;
import org.exoplatform.calendar.service.RemoteCalendar;
import org.exoplatform.calendar.service.RemoteCalendarService;
import org.exoplatform.calendar.service.RssData;
import org.exoplatform.calendar.service.Utils;
import org.exoplatform.calendar.service.impl.CalendarSearchServiceConnector;
import org.exoplatform.calendar.service.impl.CalendarServiceImpl;
import org.exoplatform.calendar.service.impl.EventSearchConnector;
import org.exoplatform.calendar.service.impl.JCRDataStorage;
import org.exoplatform.calendar.service.impl.NewGroupListener;
import org.exoplatform.calendar.service.impl.NewUserListener;
import org.exoplatform.calendar.service.impl.UnifiedQuery;
import org.exoplatform.commons.api.search.data.SearchContext;
import org.exoplatform.commons.api.search.data.SearchResult;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.util.IdGenerator;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.MembershipType;
import org.exoplatform.services.organization.User;
import org.exoplatform.web.controller.metadata.ControllerDescriptor;
import org.exoplatform.web.controller.metadata.DescriptorBuilder;
import org.exoplatform.web.controller.router.Router;
import org.exoplatform.web.controller.router.RouterConfigException;

/**
 * Created by The eXo Platform SARL
 * Author : Hung Nguyen
 *          hung.nguyen@exoplatform.com
 * July 3, 2008  
 */



public class TestCalendarService extends BaseCalendarServiceTestCase {
  private RepositoryService repositoryService_ ;
  private JCRDataStorage  storage_;
  private CalendarSearchServiceConnector eventSearchConnector_ ;

  public void setUp() throws Exception {
    super.setUp();
    repositoryService_ = getService(RepositoryService.class);
    eventSearchConnector_ = getService(EventSearchConnector.class);
    storage_ = ((CalendarServiceImpl)calendarService_).getDataStorage();
  }

  public void testInitServices() throws Exception{

    assertNotNull(repositoryService_) ;
    assertEquals(repositoryService_.getDefaultRepository().getConfiguration().getName(), "repository");
    assertEquals(repositoryService_.getDefaultRepository().getConfiguration().getDefaultWorkspaceName(), "portal-test");
    assertNotNull(organizationService_) ;

    assertEquals(organizationService_.getUserHandler().findAllUsers().getSize(), 8);

    assertNotNull(storage_);

    assertNotNull(storage_.getUserCalendarHome(username));

    assertNotNull(storage_.getPublicCalendarHome());

    assertNotNull(storage_.getPublicCalendarServiceHome());

    assertNotNull(calendarService_) ;
  }

  public void testRemoveUserEvent() throws Exception {
    CalendarEvent event = createUserEvent("TestRemoveUserEvent");
    String eventId = event.getId();
    String calendarId = event.getCalendarId();
    String eventCategoryId = event.getEventCategoryId();
    //before removing
    assertNotNull(calendarService_.getEventById(eventId));

    calendarService_.removeUserEvent(username,calendarId,eventId);

    //after removing
    assertNull(calendarService_.getEventById(eventId));

//    calendarService_.removeUserCalendar(username,calendarId);
//    calendarService_.removeEventCategory(username, eventCategoryId);
  }

  //mvn test -Dtest=TestCalendarService#testGetCalendarById
  public void testGetCalendarById() throws Exception {
    Calendar cal = createPrivateCalendar(username, "myCalendar", "Desscription");
    EventCategory eventCategory = createUserEventCategory(username, "eventCategoryName");

    Calendar calSave = calendarService_.getCalendarById(cal.getId());
    assertNotNull(calSave);
    assertEquals(Calendar.TYPE_PRIVATE, calendarService_.getTypeOfCalendar(username, calSave.getId()));
    assertEquals("myCalendar", calSave.getName());
    assertEquals("Desscription", calSave.getDescription());

    //TODO: remove below test
    CalendarEvent calEvent = new CalendarEvent();
    calEvent.setEventType(CalendarEvent.TYPE_EVENT);
    calEvent.setEventCategoryId(eventCategory.getId());
    calEvent.setSummary("Have a meeting");
    java.util.Calendar fromCal = java.util.Calendar.getInstance();
    fromCal.add(java.util.Calendar.MINUTE, 1);
    java.util.Calendar toCal = java.util.Calendar.getInstance();
    toCal.add(java.util.Calendar.HOUR, 1);
    toCal.add(java.util.Calendar.MINUTE, 1);
    calEvent.setFromDateTime(fromCal.getTime());
    calEvent.setToDateTime(toCal.getTime());
    calendarService_.saveUserEvent(username, cal.getId(), calEvent, true);
    CalendarEvent eventSave = calendarService_.getEventById(calEvent.getId());
    assertNotNull(eventSave);

    //calendarService_.removeEventCategory(username, eventCategory.getId());
    //assertNotNull(calendarService_.removeUserCalendar(username, calSave.getId()));

  }

  //mvn test -Dtest=TestCalendarService#testGroupData
  public void testGroupData() throws Exception{
    Collection<Group> rootGroups = organizationService_.getGroupHandler().findGroupsOfUser(username);
    assertTrue(rootGroups.size() > 0);
    Collection<Group> johnGroups = organizationService_.getGroupHandler().findGroupsOfUser("john");
    assertTrue(johnGroups.size() > 0);

  }

  private Router loadConfiguration(String path) throws IOException{
    InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
    try {
      ControllerDescriptor routerDesc = new DescriptorBuilder().build(in);
      return new Router(routerDesc);
    } catch (RouterConfigException e) {
      log.info(e.getMessage());
    }finally {
      in.close();
    }
    return null;
  }

  public void testDefaultData() throws Exception {
    String defaultEventCategoriesConfig = "Birthday,Memo,Wedding,DayOff";
    String defaultCalendarId = "NewCalendarId";
    String defaultCalendarCategoryId = "NewCalendarCategoryId";

    // Create valueParam
    ValueParam defaultCalendarIdParam = new ValueParam();
    ValueParam defaultCalendarCategoryIdParam = new ValueParam();
    ValueParam defaultEventCategoriesConfigParam = new ValueParam();
    defaultCalendarIdParam.setValue(defaultCalendarId);
    defaultCalendarCategoryIdParam.setValue(defaultCalendarCategoryId);
    defaultEventCategoriesConfigParam.setValue(defaultEventCategoriesConfig);

    // Init config
    InitParams params = new InitParams();
    params.put(NewUserListener.EVENT_CATEGORIES, defaultEventCategoriesConfigParam);
    NewUserListener newUserListener = new NewUserListener(calendarService_, params);
    organizationService_.addListenerPlugin(newUserListener);

    // Create new user
    String newUserName = "testUser";
    User newUser = organizationService_.getUserHandler().createUserInstance(newUserName);
    organizationService_.getUserHandler().createUser(newUser, true);

    // Create event category list from config
    String[] configValues = defaultEventCategoriesConfig.split(Utils.COMMA);
    List<String> defaultEventCategories = new ArrayList<String>();
    defaultEventCategories.add(NewUserListener.DEFAULT_EVENTCATEGORY_ID_ALL);
    for (int i = 0; i < configValues.length; i++) {
      defaultEventCategories.add(configValues[i].trim());
    }



    // Test default calendar
    List<Calendar> calendars = calendarService_.getUserCalendars(newUserName, true);
    assertEquals(calendars.size(),1);
    assertEquals(calendars.get(0).getName(),newUserName);

    // Test default event categories
    List<EventCategory> eventCategories = calendarService_.getEventCategories(newUserName);
    for (EventCategory eventCategory : eventCategories) {
      calendarService_.removeEventCategory(newUserName,eventCategory.getId());
    }
    eventCategories = calendarService_.getEventCategories(newUserName);
    assertEquals(eventCategories.size(), 0);

    calendarService_.removeUserCalendar(newUserName, defaultCalendarId);
    organizationService_.getUserHandler().removeUser(newUserName, true);

  }

  public void testCalendar() throws Exception {

    // create/get calendar in private folder
    Calendar cal = new Calendar();
    cal.setName("myCalendar");
    cal.setDescription("Desscription");
    cal.setPublic(true);
    calendarService_.saveUserCalendar(username, cal, true);
    Calendar myCal = calendarService_.getUserCalendar(username, cal.getId());
    assertNotNull(myCal);
    assertEquals(myCal.getName(), "myCalendar");

    // create/get calendar in public folder
    cal.setPublic(false);
    cal.setGroups(new String[] { "users", "admin" });
    cal.setViewPermission(new String[] { "member:/users", "member:/admin" });
    cal.setEditPermission(new String[] { "admin" });
    calendarService_.savePublicCalendar(cal, true);
    Calendar publicCal = calendarService_.getGroupCalendar(cal.getId());
    assertNotNull(publicCal);
    assertEquals(publicCal.getName(), "myCalendar");

    // get calendar in public folder by groupId
    List<GroupCalendarData> groupCalendarList = calendarService_.getGroupCalendars(new String[] { "users" }, true, username);
    assertNotNull(groupCalendarList);
    assertEquals(groupCalendarList.size(), 1);

    groupCalendarList = calendarService_.getGroupCalendars(new String[] { "admin" }, true, username);
    assertNotNull(groupCalendarList);
    assertEquals(groupCalendarList.size(), 1);

    groupCalendarList = calendarService_.getGroupCalendars(new String[] { "admin1" }, true, username);
    assertNotNull(groupCalendarList);
    assertEquals(groupCalendarList.size(), 0);

    // update public calendar
    cal.setPublic(false);
    cal.setName("myCalendarUpdated");
    calendarService_.savePublicCalendar(cal, false);
    myCal = calendarService_.getGroupCalendar(cal.getId());
    assertEquals(myCal.getName(), "myCalendarUpdated");

    // remove public calendar
    Calendar removeCal = calendarService_.removePublicCalendar(cal.getId());
    assertEquals(removeCal.getName(), "myCalendarUpdated");

    // remove private calendar
    removeCal = calendarService_.removeUserCalendar(username, cal.getId());
    assertEquals(removeCal.getName(), "myCalendar");

    // calendar setting
    CalendarSetting setting = new CalendarSetting();
    setting.setBaseURL("url");
    calendarService_.saveCalendarSetting(username, setting);
    assertEquals("url", calendarService_.getCalendarSetting(username).getBaseURL());
  }


  public void testEventCategory() throws Exception {
    Calendar cal = new Calendar();
    cal.setName("myCalendar");
    cal.setDescription("Desscription");
    cal.setPublic(true);
    // create/get calendar in private folder
    calendarService_.saveUserCalendar(username, cal, true);
    Calendar myCal = calendarService_.getUserCalendar(username, cal.getId());
    assertNotNull(myCal);
    assertEquals(myCal.getName(), "myCalendar");

    EventCategory eventCategory = new EventCategory();
    String name = "eventCategoryName";
    eventCategory.setName(name);
    calendarService_.saveEventCategory(username, eventCategory, true);
    assertEquals(1, calendarService_.getEventCategories(username).size());
    assertNotNull(calendarService_.getEventCategory(username, eventCategory.getId()));

    // import, export calendar
    CalendarEvent calendarEvent = new CalendarEvent();
    calendarEvent.setCalendarId(cal.getId());
    calendarEvent.setSummary("sum");
    calendarEvent.setEventType(CalendarEvent.TYPE_EVENT);
    calendarEvent.setFromDateTime(new Date());
    calendarEvent.setToDateTime(new Date());
    calendarService_.saveUserEvent(username, cal.getId(), calendarEvent, true);

    List<String> calendarIds = new ArrayList<String>();
    calendarIds.add(cal.getId());
    OutputStream out = calendarService_.getCalendarImportExports(CalendarService.ICALENDAR).exportCalendar(username,
            calendarIds,
            "0",
            -1);
    ByteArrayInputStream is = new ByteArrayInputStream(out.toString().getBytes());

    assertNotNull(calendarService_.removeUserEvent(username, cal.getId(), calendarEvent.getId()));
    assertEquals(0, calendarService_.getUserEventByCalendar(username, calendarIds).size());
    assertNotNull(calendarService_.removeUserCalendar(username, cal.getId()));

    calendarService_.getCalendarImportExports(CalendarService.ICALENDAR).importCalendar(username,
            is,
            null,
            "importedCalendar",
            null,
            null,
            true);
    List<Calendar> cals = calendarService_.getUserCalendars(username, true);
    List<String> newCalendarIds = new ArrayList<String>();
    for (Calendar calendar : cals)
      newCalendarIds.add(calendar.getId());
    List<CalendarEvent> events = calendarService_.getUserEventByCalendar(username, newCalendarIds);
    assertEquals(events.get(0).getSummary(), "sum");
    calendarService_.removeUserEvent(username, events.get(0).getCalendarId(), events.get(0).getId()) ;
    // remove Event category
    calendarService_.removeEventCategory(username, eventCategory.getId());

    assertNotNull(calendarService_.removeUserCalendar(username, newCalendarIds.get(0)));
  }

  public void testPublicEvent() throws Exception {
    Calendar cal = new Calendar();
    cal.setName("CalendarName");
    cal.setDescription("CalendarDesscription");
    cal.setPublic(true);
    calendarService_.savePublicCalendar(cal, true);

    EventCategory eventCategory = new EventCategory();
    eventCategory.setName("EventCategoryName1");
    calendarService_.saveEventCategory(username, eventCategory, true);

    CalendarEvent calEvent = new CalendarEvent();
    calEvent.setEventCategoryId(eventCategory.getId());
    calEvent.setSummary("Have a meeting");
    java.util.Calendar fromCal = java.util.Calendar.getInstance();
    java.util.Calendar toCal = java.util.Calendar.getInstance();
    toCal.add(java.util.Calendar.HOUR, 1);
    calEvent.setFromDateTime(fromCal.getTime());
    calEvent.setToDateTime(toCal.getTime());
    calendarService_.savePublicEvent(cal.getId(), calEvent, true);

    assertNotNull(calendarService_.getGroupEvent(calEvent.getId()));
    List<String> calendarIds = new ArrayList<String>();
    calendarIds.add(cal.getId());
    assertEquals(1, calendarService_.getGroupEventByCalendar(calendarIds).size());
    assertNotNull(calendarService_.removePublicEvent(cal.getId(), calEvent.getId()));

    calendarService_.removeEventCategory(username, eventCategory.getId());
    calendarService_.removeUserCalendar(username, cal.getId());
  }

  public void testPrivateEvent() throws Exception {
    Calendar cal = new Calendar();
    cal.setName("CalendarName");
    cal.setDescription("CalendarDescription");
    cal.setPublic(false);
    calendarService_.saveUserCalendar(username, cal, true);

    EventCategory eventCategory = new EventCategory();
    eventCategory.setName("EventCategoryName2");
    calendarService_.saveEventCategory(username, eventCategory, true);

    CalendarEvent calEvent = new CalendarEvent();
    calEvent.setEventCategoryId(eventCategory.getId());
    calEvent.setSummary("Have a meeting");
    java.util.Calendar fromCal = java.util.Calendar.getInstance();
    java.util.Calendar toCal = java.util.Calendar.getInstance();
    toCal.add(java.util.Calendar.HOUR, 1);
    calEvent.setFromDateTime(fromCal.getTime());
    calEvent.setToDateTime(toCal.getTime());
    calendarService_.saveUserEvent(username, cal.getId(), calEvent, true);

    EventQuery query = new EventQuery();
    query.setCategoryId(new String[] { eventCategory.getId() });
    assertEquals(calendarService_.getUserEvents(username, query).size(), 1);

    EventQuery eventQuery = new EventQuery();
    eventQuery.setText("Have a meeting");

    assertEquals(1, calendarService_.searchEvent(username, eventQuery, new String[] {}).getAll().size());
    assertEquals(1, calendarService_.getEvents(username, eventQuery, new String[] {}).size());

    List<CalendarEvent> list = new ArrayList<CalendarEvent>();
    list.add(calEvent);
    Calendar movedCal = new Calendar();
    movedCal.setName("MovedCalendarName");
    movedCal.setDescription("CalendarDescription");
    movedCal.setPublic(false);
    calendarService_.saveUserCalendar(username, movedCal, true);

    calendarService_.moveEvent(cal.getId(), movedCal.getId(), calEvent.getCalType(), calEvent.getCalType(), list, username);
    eventQuery = new EventQuery();
    eventQuery.setCalendarId(new String[] { movedCal.getId() });
    assertEquals(1, calendarService_.getEvents(username, eventQuery, new String[] {}).size());

    calendarService_.removeEventCategory(username, eventCategory.getId());
    calendarService_.removeUserCalendar(username, cal.getId());
  }

  public void testLastUpdatedTime() throws Exception {
    Calendar cal = new Calendar();
    cal.setName("CalendarName");
    cal.setDescription("CalendarDesscription");
    cal.setPublic(true);
    calendarService_.savePublicCalendar(cal, true);

    EventCategory eventCategory = new EventCategory();
    eventCategory.setName("LastUpdatedTimeEventCategoryName");
    calendarService_.saveEventCategory(username, eventCategory, true);

    CalendarEvent calEvent = new CalendarEvent();
    calEvent.setEventCategoryId(eventCategory.getId());
    calEvent.setCalendarId(cal.getId());
    calEvent.setSummary("Have a meeting");
    java.util.Calendar fromCal = java.util.Calendar.getInstance();
    java.util.Calendar toCal = java.util.Calendar.getInstance();
    toCal.add(java.util.Calendar.HOUR, 1);
    calEvent.setFromDateTime(fromCal.getTime());
    calEvent.setToDateTime(toCal.getTime());
    calendarService_.savePublicEvent(cal.getId(), calEvent, true);

    CalendarEvent event = calendarService_.getGroupEvent(cal.getId(), calEvent.getId());
    Date createdDate = event.getLastUpdatedTime();
    assertNotNull(createdDate);
    event.setSummary("Have a new meeting");
    calendarService_.savePublicEvent(cal.getId(), event, false);
    Date modifiedDate = calendarService_.getGroupEvent(cal.getId(), event.getId()).getLastUpdatedTime();
    assertNotNull(modifiedDate);
    assertTrue(modifiedDate.after(createdDate));

    calendarService_.removeEventCategory(username, eventCategory.getId());
    calendarService_.removePublicCalendar(cal.getId());
  }

  public void testFeed() throws Exception {
    Calendar cal = new Calendar();
    cal.setName("CalendarName");
    cal.setPublic(false);
    calendarService_.saveUserCalendar(username, cal, true);

    EventCategory eventCategory = new EventCategory();
    eventCategory.setName("EventCategoryName3");
    calendarService_.saveEventCategory(username, eventCategory, true);

    CalendarEvent calEvent = new CalendarEvent();
    calEvent.setEventCategoryId(eventCategory.getId());
    calEvent.setSummary("Have a meeting");
    java.util.Calendar fromCal = java.util.Calendar.getInstance();
    java.util.Calendar toCal = java.util.Calendar.getInstance();
    toCal.add(java.util.Calendar.HOUR, 1);
    calEvent.setFromDateTime(fromCal.getTime());
    calEvent.setToDateTime(toCal.getTime());
    calendarService_.saveUserEvent(username, cal.getId(), calEvent, true);

    LinkedHashMap<String, Calendar> calendars = new LinkedHashMap<String, Calendar>();
    calendars.put(Utils.PRIVATE_TYPE + Utils.COLON + cal.getId(), cal);
    RssData rssData = new RssData();

    String name = "RSS";
    rssData.setName(name + Utils.RSS_EXT);
    String url = "http://localhost:8080/csdemo/rest-csdemo/cs/calendar/feed/" + username + Utils.SLASH + name + Utils.SLASH
            + IdGenerator.generate() + Utils.RSS_EXT;
    rssData.setUrl(url);
    rssData.setTitle(name);
    rssData.setDescription("Description");
    rssData.setLink(url);
    rssData.setVersion("rss_2.0");

    calendarService_.generateRss(username, calendars, rssData);
    assertEquals(1, calendarService_.getFeeds(username).size());
    calendarService_.removeFeedData(username, name);
    assertEquals(0, calendarService_.getFeeds(username).size());

    calendarService_.removeEventCategory(username, eventCategory.getId());
    calendarService_.removeUserCalendar(username, cal.getId());
  }

  public void testRemoteCalendar() throws Exception {
    String remoteUrl = "http://www.google.com/calendar/ical/exomailtest@gmail.com/private-462ee65e38f964b0aa64a37b427ed673/basic.ics";

    // test Remote ICS
    RemoteCalendarService remoteCalendarService = calendarService_.getRemoteCalendarService();
    RemoteCalendar remoteCal = new RemoteCalendar();
    remoteCal.setType(CalendarService.ICALENDAR);
    remoteCal.setUsername(username);
    remoteCal.setRemoteUrl(remoteUrl);
    remoteCal.setCalendarName("CalendarName");
    remoteCal.setDescription("Description");
    remoteCal.setSyncPeriod("Auto");
    remoteCal.setBeforeDate(0);
    remoteCal.setAfterDate(0);
    Calendar cal;
    try {
      cal = remoteCalendarService.importRemoteCalendar(remoteCal);
    } catch (IOException e) {
      log.info("Exception occurs when connect to remote calendar. Skip this test.");
      return;
    }
    //cal.setCategoryId(calCategory.getId());
    calendarService_.saveUserCalendar(username, cal, true);

    Calendar newCal = calendarService_.getCalendarById(cal.getId());
    assertNotNull(newCal);
    assertEquals(newCal.getId(), cal.getId());
    //List<CalendarEvent> events = calendarService_.getUserEventByCalendar(username, Arrays.asList(cal.getId()));
    //assertTrue(events.size() > 0);

    boolean isRemoteCalendar = calendarService_.isRemoteCalendar(username, cal.getId());
    assertTrue(isRemoteCalendar);

    RemoteCalendar remoteCalendar = calendarService_.getRemoteCalendar(username, cal.getId());
    assertEquals(remoteUrl, remoteCalendar.getRemoteUrl());

    Calendar calendar1 = calendarService_.getRemoteCalendar(username, remoteUrl, CalendarService.ICALENDAR);
    assertEquals(cal.getId(), calendar1.getId());

    int remoteCalendarCount = calendarService_.getRemoteCalendarCount(username);
    assertEquals(1, remoteCalendarCount);

    String newRemoteUrl = "https://www.google.com/calendar/dav/exomailtest@gmail.com/events/";
    remoteCalendar.setRemoteUrl(newRemoteUrl);
    calendarService_.updateRemoteCalendarInfo(remoteCalendar);

    remoteCalendar = calendarService_.getRemoteCalendar(username, cal.getId());
    assertEquals(newRemoteUrl, remoteCalendar.getRemoteUrl());

    calendarService_.removeUserCalendar(username, cal.getId());

    // test RemoteCaldav
    remoteCal.setType(CalendarService.CALDAV);
    remoteCal.setRemoteUser("exomailtest@gmail.com");
    remoteCal.setRemotePassword("tuanpham");
    remoteCal.setRemoteUrl("https://www.google.com/calendar/dav/exomailtest@gmail.com/events/");
    try {
      cal = remoteCalendarService.importRemoteCalendar(remoteCal);
    } catch (Exception e) {
      if(log.isDebugEnabled())
      log.info("Exception occurs when connect to remote calendar. Skip this test.");
      return;
    }

    List<CalendarEvent> events1 = calendarService_.getUserEventByCalendar(username, Arrays.asList(cal.getId()));
    assertTrue(events1.size() > 0);
    calendarService_.removeUserCalendar(username, cal.getId());
  }

  public void testGetUserCalendar() {
    try {
      Calendar calendar = calendarService_.getUserCalendar(username, "Not exist calendar");
      assertNull(calendar);
    } catch (Exception e) {
      fail();
    }
  }

  public void testSaveUserCalendar() {
    try {
      Calendar calendar = createPrivateCalendar(username, "CalendarName", "CalendarDesscription");

      // Edit calendar
      String newCalendarName = "CalendarName edited";
      calendar.setName(newCalendarName);

      calendarService_.saveUserCalendar(username, calendar, false);
      Calendar edidedCalendar = calendarService_.getUserCalendar(username, calendar.getId()) ;
      assertEquals(newCalendarName, edidedCalendar.getName());

      calendarService_.removeUserCalendar(username, calendar.getId());
    } catch (Exception e) {
      //fail();
    }
  }

  public void testSavePublicCalendar() {
    try {
      Calendar calendar = createPublicCalendar(new String[]{"/platform/users", "/organization/management/executive-board"}, "CalendarName", "CalendarDesscription");
      assertNotNull(calendarService_.getGroupCalendar(calendar.getId())) ;
      assertEquals(calendar, calendarService_.getCalendarById(calendar.getId()));

    } catch (Exception e) {
      fail();
    }

  }


  public void testSaveEventCategory() {
    try {
      Calendar calendar = createPrivateCalendar(username, "myCalendar", "Description");
      Calendar publicCalendar = createPublicCalendar(userGroups, "publicCalendar", "publicDescription");

      String eventCategoryName = "eventCategoryName1";
      EventCategory eventCategory = createUserEventCategory(username, eventCategoryName);

      java.util.Calendar fromCal = java.util.Calendar.getInstance();
      java.util.Calendar toCal = java.util.Calendar.getInstance();
      toCal.add(java.util.Calendar.HOUR, 1);

      // Create user event
      CalendarEvent userEvent = createUserEvent(calendar.getId(), eventCategory, "Have a meeting", fromCal, toCal);

      // Create public event
      CalendarEvent publicEvent = createPublicEvent(publicCalendar.getId(), eventCategory, "Have a meeting", fromCal, toCal);

      // Edit event category
      String newEventCategoryName = "newEventCategoryName";
      eventCategory.setName(newEventCategoryName);
      calendarService_.saveEventCategory(username, eventCategory, false);

      // Check edited event category
      EventCategory edidedEventCategory = calendarService_.getEventCategory(username, eventCategory.getId());
      assertNotNull(edidedEventCategory);
      assertEquals(newEventCategoryName, edidedEventCategory.getName());

      calendarService_.removeUserEvent(username, calendar.getId(), userEvent.getId());
      calendarService_.removePublicEvent(publicCalendar.getId(), publicEvent.getId());
      calendarService_.removeEventCategory(username, eventCategory.getId());
      calendarService_.removeUserCalendar(username, calendar.getId());
      calendarService_.removePublicCalendar(publicCalendar.getId());
    } catch (Exception e) {
      fail();
    }
  }

  public void testRemoveEventCategory() {
    try {
      Calendar calendar = createPrivateCalendar(username, "myCalendar", "Description");
      Calendar publicCalendar = createPublicCalendar(userGroups, "publicCalendar", "publicDescription");

      EventCategory eventCategory = createUserEventCategory(username, "eventCategoryName2");

      java.util.Calendar fromCal = java.util.Calendar.getInstance();
      java.util.Calendar toCal = java.util.Calendar.getInstance();
      toCal.add(java.util.Calendar.HOUR, 1);

      CalendarEvent userEvent = createUserEvent(calendar.getId(), eventCategory, "Have a meeting", fromCal, toCal);
      CalendarEvent publicEvent = createPublicEvent(publicCalendar.getId(), eventCategory, "Have a meeting", fromCal, toCal);

      // Remove event category
      calendarService_.removeEventCategory(username, eventCategory.getId());

      // Check removed event category
      try {
        calendarService_.getEventCategory(username, eventCategory.getId());

        // If not throw exception then fail
        fail();
      } catch (PathNotFoundException ex) {
      }

      // Check user event
      CalendarEvent calendarEvent3 = calendarService_.getEvent(username, userEvent.getId());
      assertNotNull(calendarEvent3);
      assertEquals(NewUserListener.DEFAULT_EVENTCATEGORY_ID_ALL, calendarEvent3.getEventCategoryId());
      assertEquals(NewUserListener.DEFAULT_EVENTCATEGORY_NAME_ALL, calendarEvent3.getEventCategoryName());

      // Check public event
      CalendarEvent calendarEvent4 = calendarService_.getGroupEvent(publicCalendar.getId(), publicEvent.getId());
      assertNotNull(calendarEvent4);
      assertEquals(NewUserListener.DEFAULT_EVENTCATEGORY_ID_ALL, calendarEvent4.getEventCategoryId());
      assertEquals(NewUserListener.DEFAULT_EVENTCATEGORY_NAME_ALL, calendarEvent4.getEventCategoryName());


      calendarService_.removeUserEvent(username, calendar.getId(), userEvent.getId());
      calendarService_.removePublicEvent(publicCalendar.getId(), publicEvent.getId());
      calendarService_.removeUserCalendar(username, calendar.getId());
      calendarService_.removePublicCalendar(publicCalendar.getId());
    } catch (Exception e) {
      fail();
    }
  }

  public void testGetEvent() {
    try {
      Calendar calendar = createPrivateCalendar(username, "myCalendar", "Description");

      EventCategory eventCategory = createUserEventCategory(username, "eventCategoryName3");

      java.util.Calendar fromCal = java.util.Calendar.getInstance();
      java.util.Calendar toCal = java.util.Calendar.getInstance();
      toCal.add(java.util.Calendar.HOUR, 1);

      // Create attachment
      String attachmentName = "Acttach file";
      String attachmentMinetype = "MimeType";
      Attachment attachment = new Attachment() ;
      attachment.setName(attachmentName) ;
      attachment.setInputStream(new InputStream() {
        @Override
        public int read() throws IOException {
          return 0;
        }
      }) ;
      attachment.setMimeType(attachmentMinetype) ;

      // Create reminder
      String reminderType = Reminder.TYPE_BOTH;
      long reminderAlarmBefore = new Date().getTime();
      String reminderEmailAddress = "abc@gmail.com";
      Reminder reminder = new Reminder(reminderType);
      reminder.setAlarmBefore(reminderAlarmBefore);
      reminder.setEmailAddress(reminderEmailAddress);
      reminder.setRepeate(false);

      // Create and save event
      String eventSummay = "Have a meeting";
      CalendarEvent calendarEvent = new CalendarEvent();
      calendarEvent.setEventCategoryId(eventCategory.getId());
      calendarEvent.setEventCategoryName(eventCategory.getName());
      calendarEvent.setSummary(eventSummay);
      calendarEvent.setFromDateTime(fromCal.getTime());
      calendarEvent.setToDateTime(toCal.getTime());
      calendarEvent.setAttachment(Arrays.asList(attachment));
      calendarEvent.setReminders(Arrays.asList(reminder));
      calendarService_.saveUserEvent(username, calendar.getId(), calendarEvent, true);

      CalendarEvent findEvent = calendarService_.getEvent(username, calendarEvent.getId());
      assertNotNull(findEvent);
      assertEquals(eventSummay, findEvent.getSummary());

      // Check attachment
      List<Attachment> attachments = findEvent.getAttachment();
      assertNotNull(attachments);
      assertEquals(1, attachments.size());
      Attachment eventAttachment = attachments.get(0);
      assertEquals(attachmentName, eventAttachment.getName());
      assertEquals(attachmentMinetype, eventAttachment.getMimeType());

      // Check reminder
      List<Reminder> reminders = findEvent.getReminders();
      assertNotNull(reminders);
      assertEquals(1, reminders.size());
      Reminder eventReminder = reminders.get(0);
      assertEquals(reminderType, eventReminder.getReminderType());
      assertEquals(reminderAlarmBefore, eventReminder.getAlarmBefore());
      assertEquals(reminderEmailAddress, eventReminder.getEmailAddress());
      assertEquals(false, eventReminder.isRepeat());

      // Xóa reminder
      findEvent.setReminders(null);
      calendarService_.saveUserEvent(username, calendar.getId(), findEvent, false);

      CalendarEvent findEvent1 = calendarService_.getEvent(username, calendarEvent.getId());
      assertNotNull(findEvent1);
      List<Reminder> reminders1 = findEvent1.getReminders();
      assertEquals(0, reminders1.size());

      calendarService_.removeUserEvent(username, calendar.getId(), calendarEvent.getId());
      calendarService_.removeEventCategory(username, eventCategory.getId());
      calendarService_.removeUserCalendar(username, calendar.getId());
    } catch (Exception e) {
      fail();
    }
  }

  public void testGetEventById() throws Exception {
    Calendar calendar = createPrivateCalendar(username, "myCalendar", "Description");

    EventCategory eventCategory = createUserEventCategory(username, "eventCategoryName3");
    java.util.Calendar fromCal = java.util.Calendar.getInstance();
    java.util.Calendar toCal = java.util.Calendar.getInstance();
    toCal.add(java.util.Calendar.HOUR, 1);
    // Create and save event
    String eventSummay = "Have a meeting";
    CalendarEvent calendarEvent = new CalendarEvent();
    calendarEvent.setEventCategoryId(eventCategory.getId());
    calendarEvent.setEventCategoryName(eventCategory.getName());
    calendarEvent.setSummary(eventSummay);
    calendarEvent.setFromDateTime(fromCal.getTime());
    calendarEvent.setToDateTime(toCal.getTime());
    calendarService_.saveUserEvent(username, calendar.getId(), calendarEvent, true);

    CalendarEvent findEvent1 = calendarService_.getEventById(calendarEvent.getId());
    assertNotNull(findEvent1);

    calendarService_.removeUserEvent(username, calendar.getId(), calendarEvent.getId());
    calendarService_.removeEventCategory(username, eventCategory.getId());
    calendarService_.removeUserCalendar(username, calendar.getId());
  }


  public void testMoveEvent() {
    try {
      Calendar calendar = createPrivateCalendar(username, "myCalendar", "Description");
      Calendar publicCalendar = createPublicCalendar(userGroups, "publicCalendar", "publicDescription");

      EventCategory eventCategory = createUserEventCategory(username, "MoveEventCategory");

      java.util.Calendar fromCal = java.util.Calendar.getInstance();
      java.util.Calendar toCal = java.util.Calendar.getInstance();
      toCal.add(java.util.Calendar.HOUR, 1);

      CalendarEvent event = createPublicEvent(publicCalendar.getId(), eventCategory, "Have a meeting", fromCal, toCal);

      List<CalendarEvent> events = new ArrayList<CalendarEvent>();
      events.add(event);

      calendarService_.moveEvent(publicCalendar.getId(),
              calendar.getId(),
              String.valueOf(Calendar.TYPE_PUBLIC),
              String.valueOf(Calendar.TYPE_PRIVATE),
              events,
              username);

      CalendarEvent userEvent = calendarService_.getEvent(username, event.getId());
      assertNotNull(userEvent);

      List<CalendarEvent> events1 = new ArrayList<CalendarEvent>();
      events1.add(userEvent);
      calendarService_.moveEvent(calendar.getId(),
              publicCalendar.getId(),
              String.valueOf(Calendar.TYPE_PRIVATE),
              String.valueOf(Calendar.TYPE_PUBLIC),
              events1,
              username);


      CalendarEvent publicEvent = calendarService_.getGroupEvent(publicCalendar.getId(), userEvent.getId());
      assertNotNull(publicEvent);

      calendarService_.removeUserEvent(username, calendar.getId(), userEvent.getId());
      calendarService_.removePublicEvent(publicCalendar.getId(), publicEvent.getId());
      calendarService_.removeEventCategory(username, eventCategory.getId());
      calendarService_.removeUserCalendar(username, calendar.getId());
      calendarService_.removePublicCalendar(publicCalendar.getId());
    } catch (Exception e) {
      fail();
    }
  }

  public void testCheckFreeBusy() {
    try {
      Calendar publicCalendar = createPublicCalendar(userGroups, "publicCalendar", "publicDescription");

      EventCategory eventCategory = createUserEventCategory(username, "CheckFreeBusyCategory");

      java.util.Calendar fromCal = java.util.Calendar.getInstance();
      java.util.Calendar toCal = java.util.Calendar.getInstance();
      toCal.add(java.util.Calendar.DATE, 1);
      CalendarEvent publicEvent = createPublicEvent(publicCalendar.getId(), eventCategory, "Have a meeting", fromCal, toCal);

      toCal.add(java.util.Calendar.DATE, 1);

      EventQuery eventQuery = new EventQuery();
      eventQuery.setFromDate(fromCal);
      eventQuery.setToDate(toCal);
      eventQuery.setParticipants(new String[] { "root" });
      eventQuery.setNodeType("exo:calendarPublicEvent");
      Map<String, String> parsMap = calendarService_.checkFreeBusy(eventQuery);

      calendarService_.removePublicEvent(publicCalendar.getId(), publicEvent.getId());
      calendarService_.removeEventCategory(username, eventCategory.getId());
      calendarService_.removePublicCalendar(publicCalendar.getId());
    } catch (Exception e) {
      fail();
    }
  }

  public void testCreateSessionProvider() {
    try {
      SessionProvider sessionProvider = storage_.createSessionProvider();
      assertNotNull(sessionProvider);
    } catch (Exception e) {
      fail();
    }
  }

  //mvn test -Dtest=TestCalendarService#testGetPublicEvents
  public void testGetPublicEvents() {
    try {
      Calendar publicCalendar = createPublicCalendar(userGroups, "publicCalendar", "publicDescription");

      EventCategory eventCategory = createUserEventCategory(username, "GetPublicEventsCategory");

      java.util.Calendar fromCal = java.util.Calendar.getInstance();
      fromCal.add(java.util.Calendar.HOUR, 1);
      java.util.Calendar toCal = java.util.Calendar.getInstance();
      toCal.add(java.util.Calendar.HOUR, 2);

      CalendarEvent publicEvent = createPublicEvent(publicCalendar.getId(), eventCategory, "Have a meeting", fromCal, toCal);
      publicEvent.setPrivate(false);
      calendarService_.savePublicEvent(publicCalendar.getId(), publicEvent, false);
      publicEvent.setEventType(CalendarEvent.TYPE_EVENT);
      EventQuery eventQuery = new EventQuery();
      eventQuery.setCalendarId(new String[] {publicCalendar.getId()});
      List<CalendarEvent> events = calendarService_.getPublicEvents(eventQuery);
      assertEquals(1, events.size());
      CalendarEvent resultEvent = events.get(0);
      assertEquals(publicEvent.getId(), resultEvent.getId());
      assertEquals(publicEvent.getSummary(), resultEvent.getSummary());

      //Test search space event
//      List<MembershipEntry> entries = new LinkedList<MembershipEntry>();
//      entries.add(new MembershipEntry("/platform/users"));
//      entries.add(new MembershipEntry("/organization/management/executive-board"));
      
      login("john");
      EventQuery query = new UnifiedQuery();
      query.setText("  \" a meeting\" ");
      query.setOrderType(Utils.ORDER_TYPE_ASCENDING);
      query.setOrderBy(new String[]{Utils.ORDERBY_TITLE});
      Collection<String> params = new ArrayList<String>();
      Collection<SearchResult> rs = eventSearchConnector_.search(null, query.getText(), params, 0, 10, query.getOrderBy()[0] , query.getOrderType());
      assertEquals(1, rs.size());
      checkFields(rs.iterator().next());

      login(username);

      rs = eventSearchConnector_.search(null, query.getText(), params, 0, 10, query.getOrderBy()[0] , query.getOrderType());
      assertEquals(1, rs.size());
      checkFields(rs.iterator().next());

      //update group calendar 
      publicCalendar = calendarService_.getGroupCalendar(publicCalendar.getId());
      publicCalendar.setGroups(new String[]{"/platform/guests"});

      calendarService_.savePublicCalendar(publicCalendar, false);
      rs = eventSearchConnector_.search(null, query.getText(), params, 0, 10, query.getOrderBy()[0] , query.getOrderType());
      assertEquals(0, rs.size());

      login("demo");
      rs = eventSearchConnector_.search(null, query.getText(), params, 0, 10, query.getOrderBy()[0] , query.getOrderType());
      assertEquals(1, rs.size());

      //update to space calendar 
      organizationService_.getGroupHandler().addGroupEventListener(new NewGroupListener(calendarService_, new InitParams()));
      Group parent = organizationService_.getGroupHandler().findGroupById("/spaces");
      Group g = organizationService_.getGroupHandler().createGroupInstance();
      g.setGroupName("spacetest");
      g.setLabel("Calendar Space");
      g.setDescription("simulate space creted");
      organizationService_.getGroupHandler().addChild(parent, g, true);
      g = organizationService_.getGroupHandler().findGroupById(g.getId());
      Collection<Group> gr = organizationService_.getGroupHandler().findGroupsOfUser("raul");
      assertEquals(1,gr.size());
      User u = organizationService_.getUserHandler().findUserByName("raul");
      MembershipType m = (MembershipType)organizationService_.getMembershipTypeHandler().findMembershipTypes().toArray()[0];
      organizationService_.getMembershipHandler().linkMembership(u, g, m, false);
      gr = organizationService_.getGroupHandler().findGroupsOfUser("raul");
      assertEquals(2,gr.size());

      List<GroupCalendarData> spaceCals = calendarService_.getGroupCalendars(new String[]{"/spaces/spacetest"}, true, "raul");
      //success save calendar by NewGroupListener;
      assertEquals(1, spaceCals.get(0).getCalendars().size());
      Calendar spaceCal = spaceCals.get(0).getCalendars().get(0) ;
      //spaceCal.setId(g.getGroupName() + Utils.SPACE_ID_PREFIX);
      //calendarService_.savePublicCalendar(spaceCal, true);
      publicEvent.setCalendarId(spaceCal.getId());
      publicEvent.setSummary("space event created");
      calendarService_.savePublicEvent(spaceCal.getId(), publicEvent, true);
      query.setText("\"space event\"");
      login("raul");
      SearchContext sc = new SearchContext(loadConfiguration("conf/portal/controller.xml"), "classic");
      assertNotNull(sc);

      //Case build url for event detail in calendar
      rs = eventSearchConnector_.search(sc, query.getText(), params, 0, 10, query.getOrderBy()[0] , query.getOrderType());
      assertEquals(1, rs.size());
      SearchResult item = rs.iterator().next();
      checkFields(item);
      assertEquals("/portal/classic/calendar/details/" + publicEvent.getId(), item.getUrl());

      calendarService_.removePublicEvent(publicCalendar.getId(), publicEvent.getId());
      calendarService_.removeEventCategory(username, eventCategory.getId());
      calendarService_.removePublicCalendar(publicCalendar.getId());
    } catch (Exception ex) {
      fail();
    }
  }
    private void checkFields(SearchResult item) {
        assertNotNull(item.getTitle()) ;
        assertNotNull(item.getExcerpt()) ;
        assertNotNull(item.getDetail()) ;
        assertNull(item.getImageUrl());
        assertNotNull(item.getUrl());
        assertEquals(true, item.getDate() > 0);
    }

  private CalendarEvent createUserEvent(String calendarId,
                                        EventCategory eventCategory,
                                        String summary,
                                        java.util.Calendar fromCal,
                                        java.util.Calendar toCal) {
    try {
      CalendarEvent calendarEvent = new CalendarEvent();
      calendarEvent.setEventCategoryId(eventCategory.getId());
      calendarEvent.setEventCategoryName(eventCategory.getName());
      calendarEvent.setSummary(summary);
      calendarEvent.setFromDateTime(fromCal.getTime());
      calendarEvent.setToDateTime(toCal.getTime());
      calendarService_.saveUserEvent(username, calendarId, calendarEvent, true);
      return calendarEvent;
    } catch (Exception e) {
      fail();
      return null;
    }
  }

  private CalendarEvent createUserEvent(String summary) throws Exception{
    CalendarEvent event = createCalendarEventInstance(summary);
    Calendar calendar = createPrivateCalendar(username, "CalendarTest","CalendarTest");
    EventCategory category = createUserEventCategory(username, "CalendarCategoryTest");
    java.util.Calendar from = java.util.Calendar.getInstance();
    java.util.Calendar to   = java.util.Calendar.getInstance();
    to.add(java.util.Calendar.HOUR, 1);
    return createUserEvent(calendar.getId(),category,summary,from, to);
  }

  private CalendarEvent createPublicEvent(String publicCalendarId,
                                          EventCategory eventCategory,
                                          String summary,
                                          java.util.Calendar fromCal,
                                          java.util.Calendar toCal) {
    try {
      CalendarEvent publicEvent = new CalendarEvent();
      publicEvent.setEventCategoryId(eventCategory.getId());
      publicEvent.setEventCategoryName(eventCategory.getName());
      publicEvent.setSummary("Have a meeting");
      publicEvent.setFromDateTime(fromCal.getTime());
      publicEvent.setToDateTime(toCal.getTime());
      calendarService_.savePublicEvent(publicCalendarId, publicEvent, true);
      return publicEvent;
    } catch (Exception e) {
      fail();
      return null;
    }
  }
}
