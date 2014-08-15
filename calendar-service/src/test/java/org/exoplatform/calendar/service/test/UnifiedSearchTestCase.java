/*
 * Copyright (C) 2012 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.calendar.service.test;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.jcr.query.Query;

import org.exoplatform.calendar.service.Calendar;
import org.exoplatform.calendar.service.CalendarEvent;
import org.exoplatform.calendar.service.CalendarSetting;
import org.exoplatform.calendar.service.EventCategory;
import org.exoplatform.calendar.service.EventQuery;
import org.exoplatform.calendar.service.Utils;
import org.exoplatform.calendar.service.impl.CalendarSearchResult;
import org.exoplatform.calendar.service.impl.CalendarSearchServiceConnector;
import org.exoplatform.calendar.service.impl.EventSearchConnector;
import org.exoplatform.calendar.service.impl.TaskSearchConnector;
import org.exoplatform.calendar.service.impl.UnifiedQuery;
import org.exoplatform.commons.api.search.data.SearchContext;
import org.exoplatform.commons.api.search.data.SearchResult;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.web.controller.metadata.ControllerDescriptor;
import org.exoplatform.web.controller.metadata.DescriptorBuilder;
import org.exoplatform.web.controller.router.Router;
import org.exoplatform.web.controller.router.RouterConfigException;

public class UnifiedSearchTestCase extends BaseCalendarServiceTestCase {

    private CalendarSearchServiceConnector unifiedSearchService_;
    private CalendarSearchServiceConnector eventSearchConnector_;
    private CalendarSearchServiceConnector taskSearchConnector_;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        unifiedSearchService_ = getService(CalendarSearchServiceConnector.class);
        taskSearchConnector_ = getService(TaskSearchConnector.class);
        eventSearchConnector_ = getService(EventSearchConnector.class);
    }

    //mvn test -Dtest=TestCalendarService#testUnifiedSeach
    public void testUnifiedSeach() throws Exception {
        String user2 = "john" ;
        //Simple case
        String keyword = "hello \"how are\" you " ;
        EventQuery query = new UnifiedQuery() ;
        query.setQueryType(Query.SQL);
        query.setText(keyword) ;
        assertNotNull(query.getText());
        assertNotNull(query.getQueryStatement());
        login(username) ;
        CalendarSetting calSetting = calendarService_.getCalendarSetting(username) ;
        TimeZone serverZone = TimeZone.getDefault();
        assertNotNull(calSetting);
        assertEquals(serverZone.getID(), calSetting.getTimeZone());
        query.setOrderType(Utils.ORDER_TYPE_ASCENDING);
        query.setOrderBy(new String[]{Utils.ORDERBY_TITLE});
        Collection<String> params = new ArrayList<String>();
        Collection<SearchResult> result = unifiedSearchService_.search(null, query.getText(), params, 0, 10, query.getOrderBy()[0] , query.getOrderType());
        assertNotNull(result) ;
        assertEquals(0, result.size());

        //Complex case
        keyword = "hello \" i am  a \" new guy" ;
        List<String> formated = UnifiedQuery.parse(keyword) ;
        assertEquals(4, formated.size());
        keyword = keyword + " \" why don't \"we talk \" ";
        formated = UnifiedQuery.parse(keyword) ;
        assertEquals(8, formated.size());

        //create/get calendar in private folder
        Calendar cal = new Calendar();
        cal.setTimeZone(calSetting.getTimeZone());
        cal.setName("myCalendar");
        cal.setDescription("Desscription");
        //cal.setCategoryId();
        cal.setPublic(true);
        calendarService_.saveUserCalendar(username, cal, true);
        EventCategory eventCategory = new EventCategory();
        String name = "eventCategoryName";
        eventCategory.setName(name);
        calendarService_.saveEventCategory(username, eventCategory, true);

        //=Test search generic type=//
        CalendarEvent calEvent = new CalendarEvent();
        calEvent.setEventType(CalendarEvent.TYPE_EVENT);
        //calEvent.setCalType(CalendarEvent.TYPE_EVENT);
        calEvent.setEventCategoryId(eventCategory.getId());
        //Search summary
        calEvent.setSummary("do you getting Have some busy day?");
        java.util.Calendar fromCal = java.util.Calendar.getInstance();
        fromCal.add(java.util.Calendar.MINUTE, 5);
        java.util.Calendar toCal = java.util.Calendar.getInstance();
        toCal.add(java.util.Calendar.HOUR, 1);
        toCal.add(java.util.Calendar.MINUTE, 1);
        calEvent.setFromDateTime(fromCal.getTime());
        calEvent.setToDateTime(toCal.getTime());
        //calEvent.setEventState(CalendarEvent.CANCELLED);
        calendarService_.saveUserEvent(username, cal.getId(), calEvent, true);
        List<String> ids = new ArrayList<String>();
        ids.add(cal.getId());
        List<CalendarEvent> data = calendarService_.getUserEventByCalendar(username, ids) ;
        //Success to add event
        assertEquals(1,data.size()) ;

        //=Keyword to search=//
        keyword = "Have \"you getting\" busy" ;
        query.setText(keyword);
        result = unifiedSearchService_.search(null, query.getText(), params, 0, 10, query.getOrderBy()[0] , query.getOrderType());
        //Success to search
        assertEquals(1, result.size()) ;
        for(SearchResult item : result) {
            checkFieldsValueWithType(cal.getName(), calEvent, item);
        }

        //Search summary and description
        calEvent.setDescription("we have meeting with CEO");
        calendarService_.saveUserEvent(username, cal.getId(), calEvent, false);
        keyword = "do \"you getting\" CEO" ;
        query.setText(keyword);
        result = unifiedSearchService_.search(null, query.getText(), params, 0, 10, query.getOrderBy()[0] , query.getOrderType());

        //Success to search
        assertEquals(1, result.size()) ;
        for(SearchResult item : result) {
            checkFieldsValueWithType(cal.getName(), calEvent, item);
        }

        //Search summary , description and location
        calEvent.setLocation("in Hanoi");
        calendarService_.saveUserEvent(username, cal.getId(), calEvent, false);
        keyword = "hanoi CEO" ;
        query.setText(keyword);
        query.setOrderBy(new String[]{Utils.ORDERBY_DATE});
        result = unifiedSearchService_.search(null, query.getText(), params, 0, 10, query.getOrderBy()[0] , query.getOrderType());

        //Success to search
        assertEquals(1, result.size()) ;
        for(SearchResult item : result) {
            checkFieldsValueWithType(cal.getName(), calEvent, item);
        }

        //== test event search ==//
        calEvent.setEventType(CalendarEvent.TYPE_EVENT) ;
        calendarService_.saveUserEvent(username, cal.getId(), calEvent, false);
        query.setOrderBy(new String[]{Utils.ORDERBY_RELEVANCY});
        result = eventSearchConnector_.search(null, query.getText(), params, 0, 10, query.getOrderBy()[0] , query.getOrderType());
        assertEquals(1, result.size()) ;
        for(SearchResult item : result) {
            checkFieldsValueWithType(cal.getName(), calEvent, item);
        }

        //== test task search ==//
        calEvent.setEventType(CalendarEvent.TYPE_TASK);
        calendarService_.saveUserEvent(username, cal.getId(), calEvent, false);
        result = taskSearchConnector_.search(null, query.getText(), params, 0, 10, query.getOrderBy()[0] , query.getOrderType());
        assertEquals(1, result.size()) ;
        for(SearchResult item : result) {
            checkFieldsValueWithType(cal.getName(), calEvent, item);
        }

        //Test task status and icon
        calEvent.setEventType(CalendarEvent.TYPE_TASK) ;
        calendarService_.saveUserEvent(username, cal.getId(), calEvent, false);
        result = taskSearchConnector_.search(null, query.getText(), params, 0, 10, query.getOrderBy()[0] , query.getOrderType());
        assertEquals(1, result.size()) ;
        for(SearchResult item : result) {
            checkFieldsValueWithType(cal.getName(), calEvent, (CalendarSearchResult)item);
        }
        String status = CalendarEvent.COMPLETED + Utils.COLON + CalendarEvent.CANCELLED ;

        // Does not search completed task
        calEvent.setEventState(CalendarEvent.COMPLETED);
        calendarService_.saveUserEvent(username, cal.getId(), calEvent, false);
        query.setState(status);
        result = taskSearchConnector_.search(null, query.getText(), params, 0, 10, query.getOrderBy()[0] , query.getOrderType());
        assertEquals(0,result.size());

        // search all need action
        calEvent.setEventState(CalendarEvent.NEEDS_ACTION);
        calendarService_.saveUserEvent(username, cal.getId(), calEvent, false);
        query.setState(status);
        result = taskSearchConnector_.search(null, query.getText(), params, 0, 10, query.getOrderBy()[0] , query.getOrderType());
        assertEquals(1,result.size());
        CalendarSearchResult calItem = (CalendarSearchResult)result.toArray()[0] ;
        assertEquals(calEvent.getEventState(), calItem.getTaskStatus());

        // search all inprocess
        calEvent.setEventState(CalendarEvent.IN_PROCESS);
        calendarService_.saveUserEvent(username, cal.getId(), calEvent, false);
        query.setState(status);
        result = taskSearchConnector_.search(null, query.getText(), params, 0, 10, query.getOrderBy()[0] , query.getOrderType());
        assertEquals(1,result.size());
        calItem = (CalendarSearchResult)result.toArray()[0] ;
        assertEquals(calEvent.getEventState(), calItem.getTaskStatus());

        // Does not search cancelled task
        calEvent.setEventState(CalendarEvent.CANCELLED);
        calendarService_.saveUserEvent(username, cal.getId(), calEvent, false);
        query.setState(status);
        result = taskSearchConnector_.search(null, query.getText(), params, 0, 10, query.getOrderBy()[0] , query.getOrderType());
        assertEquals(0,result.size());


        //Specia case//
        calEvent.setSummary("today is friday, we will have a weekend");
        calEvent.setEventType(CalendarEvent.TYPE_EVENT) ;
        CalendarEvent calEvent2 = new CalendarEvent();
        calEvent2.setCalType(CalendarEvent.TYPE_EVENT);
        calEvent2.setFromDateTime(calEvent.getFromDateTime());
        calEvent2.setToDateTime(calEvent.getToDateTime());
        calEvent2.setSummary("Summary CEO come we will have some dayoff");
        calEvent2.setDescription("friday");
        calEvent2.setLocation("");
        calendarService_.saveUserEvent(username, cal.getId(), calEvent, false);
        calendarService_.saveUserEvent(username, cal.getId(), calEvent2, true);
        keyword = "\"we will have\" friday" ;
        query.setText(keyword);
        query.setOrderType(Utils.ORDER_TYPE_DESCENDING);
        result = eventSearchConnector_.search(null, query.getText(), params, 0, 10, query.getOrderBy()[0] , query.getOrderType());
        assertEquals(2, result.size()) ;
        SearchResult item = (SearchResult)result.toArray()[0] ;
        checkFields(item);
        SearchResult item2 = (SearchResult) result.toArray()[1] ;
        checkFields(item2);
        assertEquals(false, item2.getRelevancy() > item.getRelevancy()) ;
        query.setOrderBy(new String[]{Utils.ORDERBY_DATE});
        query.setOrderType(Utils.ORDER_TYPE_ASCENDING);
        result = eventSearchConnector_.search(null, query.getText(), params, 0, 10, query.getOrderBy()[0] , query.getOrderType());
        assertEquals(2, result.size()) ;
        CalendarSearchResult calSerResult = (CalendarSearchResult)result.toArray()[0] ;
        checkFields(calSerResult);
        checkFieldsValueWithType(cal.getName(), calEvent, calSerResult);
        CalendarSearchResult calSerResult2 = (CalendarSearchResult)result.toArray()[1] ;
        checkFields(calSerResult2);
        checkFieldsValueWithType(cal.getName(), calEvent2, calSerResult2);
        assertEquals(false, item.getDate() < item2.getDate()) ;

        //Test query filter by permission
        login(user2) ;
        Calendar johnCalendar = cal ;
        calSetting = calendarService_.getCalendarSetting(user2);
        johnCalendar.setTimeZone(calSetting.getTimeZone());
        String calendarId = "john_cal_id";
        johnCalendar.setId(calendarId) ;
        johnCalendar.setName("john calendar") ;
        calendarService_.saveUserCalendar(user2, johnCalendar, true);
        assertEquals(calendarId,calendarService_.getUserCalendar(user2, johnCalendar.getId()).getId());
        EventCategory johnEventCategory = new EventCategory();
        johnEventCategory.setName("john category");
        calendarService_.saveEventCategory(user2, eventCategory, true);
        CalendarEvent johnEvent = calEvent2 ;
        johnEvent.setId(new CalendarEvent().getId()) ;
        johnEvent.setCalendarId(johnCalendar.getId()) ;
        johnEvent.setEventType(CalendarEvent.TYPE_EVENT);
        calendarService_.saveUserEvent(user2, johnCalendar.getId(), johnEvent, true);
        assertEquals(1,calendarService_.getUserEventByCalendar(user2, Arrays.asList(new String[]{calendarId})).size());
        result = eventSearchConnector_.search(null, query.getText(), params, 0, 10, query.getOrderBy()[0] , query.getOrderType());
        assertEquals(1, result.size()) ;
        login(username);
        result = eventSearchConnector_.search(null, query.getText(), params, 0, 10, query.getOrderBy()[0] , query.getOrderType());
        assertEquals(2, result.size()) ;

        //Test case search only up coming events only
        login(user2) ;
        CalendarEvent inPassEvent = johnEvent ;
        inPassEvent.setId(new CalendarEvent().getId());
        java.util.Calendar current = java.util.Calendar.getInstance() ;
        current.add(java.util.Calendar.MINUTE, -1);
        inPassEvent.setFromDateTime(current.getTime());
        calendarService_.saveUserEvent(user2, calendarId, inPassEvent, true) ;
        assertEquals(2,calendarService_.getUserEventByCalendar(user2, Arrays.asList(new String[]{calendarId})).size());
        result = eventSearchConnector_.search(null, query.getText(), params, 0, 10, query.getOrderBy()[0] , query.getOrderType());
        assertEquals(1, result.size()) ;
        current = java.util.Calendar.getInstance();
        current.add(java.util.Calendar.MINUTE, 1);
        inPassEvent.setFromDateTime(current.getTime()) ;
        calendarService_.saveUserEvent(user2, calendarId, inPassEvent, false) ;
        assertEquals(2,calendarService_.getUserEventByCalendar(user2, Arrays.asList(new String[]{calendarId})).size());
        result = eventSearchConnector_.search(null, query.getText(), params, 0, 10, query.getOrderBy()[0] , query.getOrderType());
        assertEquals(2, result.size());

        //Search task due for and no need check from time
        current = java.util.Calendar.getInstance();
        current.add(java.util.Calendar.MINUTE, -1);
        inPassEvent.setFromDateTime(current.getTime()) ;
        current.add(java.util.Calendar.MINUTE, 2);
        inPassEvent.setToDateTime(current.getTime()) ;
        inPassEvent.setEventType(CalendarEvent.TYPE_TASK) ;
        calendarService_.saveUserEvent(user2, calendarId, inPassEvent, false) ;
        assertEquals(2,calendarService_.getUserEventByCalendar(user2, Arrays.asList(new String[]{calendarId})).size());
        result = taskSearchConnector_.search(null, query.getText(), params, 0, 10, query.getOrderBy()[0] , query.getOrderType());
        assertEquals(1, result.size());

        //Not search task in cancelled
        CalendarEvent cancelledTask = johnEvent ;
        cancelledTask.setId(new CalendarEvent().getId());
        current = java.util.Calendar.getInstance() ;
        current.add(java.util.Calendar.MINUTE, -1);
        cancelledTask.setFromDateTime(current.getTime());
        cancelledTask.setEventType(CalendarEvent.TYPE_TASK);
        cancelledTask.setEventState(CalendarEvent.CANCELLED) ;
        calendarService_.saveUserEvent(user2, calendarId, cancelledTask, true) ;
        assertEquals(3,calendarService_.getUserEventByCalendar(user2, Arrays.asList(new String[]{calendarId})).size());
        result = taskSearchConnector_.search(null, query.getText(), params, 0, 10, query.getOrderBy()[0] , query.getOrderType());
        assertEquals(1, result.size());

        //Search task not completed or not cancelled in part
        current = java.util.Calendar.getInstance() ;
        current.add(java.util.Calendar.MINUTE, -10);
        cancelledTask.setFromDateTime(current.getTime());
        cancelledTask.setEventState(CalendarEvent.NEEDS_ACTION) ;
        calendarService_.saveUserEvent(user2, calendarId, cancelledTask, false) ;
        assertEquals(3,calendarService_.getUserEventByCalendar(user2, Arrays.asList(new String[]{calendarId})).size());
        result = taskSearchConnector_.search(null, query.getText(), params, 0, 10, query.getOrderBy()[0] , query.getOrderType());
        assertEquals(2, result.size());

        String siteName = "classic";
        //test search context to build url
        SearchContext sc = new SearchContext(loadConfiguration("conf/portal/controller.xml"), siteName);
        assertNotNull(sc);
        Router rt = sc.getRouter();
        assertNotNull(rt);
        ExoContainerContext context = (ExoContainerContext)ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(ExoContainerContext.class);
        String portalName = context.getPortalContainerName();

        //router page expected return /portal/intranet/calendar
        String spaceGroupId = null;
        String url = unifiedSearchService_.getUrl(rt, portalName, siteName, spaceGroupId, Utils.PAGE_NAGVIGATION);
        assertEquals("/"+portalName+"/"+siteName+"/" + Utils.PAGE_NAGVIGATION, url);
        spaceGroupId = "/spaces/space1";
        //router space expected return /portal/g/:spaces:space1/space1/calendar
        url = unifiedSearchService_.getUrl(rt, portalName, siteName, spaceGroupId, Utils.PAGE_NAGVIGATION);
        assertEquals("/"+portalName+"/g/"+ spaceGroupId.replaceAll(Utils.SLASH, ":")+"/space1/" + Utils.PAGE_NAGVIGATION, url);
        result = eventSearchConnector_.search(sc, query.getText(), params, 0, 10, query.getOrderBy()[0] , query.getOrderType());
        assertEquals(1, result.size());
        url = unifiedSearchService_.getUrl(rt, portalName, siteName, null, Utils.PAGE_NAGVIGATION);
        for( SearchResult sr : result){
            checkFields(sr);
            assertEquals(url + Utils.SLASH + Utils.DETAIL_PATH + Utils.SLASH + sr.getUrl().split(Utils.SLASH)[sr.getUrl().split(Utils.SLASH).length-1], sr.getUrl());
        }

        //site name null
        sc = new SearchContext(loadConfiguration("conf/portal/controller.xml"), null);
        siteName = Utils.DEFAULT_SITENAME;
        spaceGroupId = null;
        url = unifiedSearchService_.getUrl(rt, portalName, siteName, spaceGroupId, Utils.PAGE_NAGVIGATION);
        assertEquals("/"+portalName+"/"+siteName+"/" + Utils.PAGE_NAGVIGATION, url);
        spaceGroupId = "/spaces/space1";
        //router space expected return /portal/g/:spaces:space1/space1/calendar
        url = unifiedSearchService_.getUrl(rt, portalName, siteName, spaceGroupId, Utils.PAGE_NAGVIGATION);
        assertEquals("/"+portalName+"/g/"+ spaceGroupId.replaceAll(Utils.SLASH, ":")+"/space1/" + Utils.PAGE_NAGVIGATION, url);
        result = eventSearchConnector_.search(sc, query.getText(), params, 0, 10, query.getOrderBy()[0] , query.getOrderType());
        assertEquals(1, result.size());
        url = unifiedSearchService_.getUrl(rt, portalName, siteName, null, "");
        for( SearchResult sr : result) {
            checkFields(sr);
            assertEquals(url + Utils.SLASH + Utils.DETAIL_PATH + Utils.SLASH + sr.getUrl().split(Utils.SLASH)[sr.getUrl().split(Utils.SLASH).length-1], sr.getUrl());
        }

        // Clean up data
        calendarService_.removeUserEvent(username, ids.get(0), calEvent.getId());
        calendarService_.removeUserEvent(username, ids.get(0), calEvent2.getId());
        calendarService_.removeUserEvent(user2, johnCalendar.getId(), johnEvent.getId());
        calendarService_.removeEventCategory(username, eventCategory.getId());
        calendarService_.removeEventCategory(user2, johnEventCategory.getId());

        assertNotNull(calendarService_.removeUserCalendar(username, ids.get(0)));
        assertNotNull(calendarService_.removeUserCalendar(user2, johnCalendar.getId()));
    }

    //mvn test -Dtest=TestCalendarService#testUnifiedSeachEx
    public void testUnifiedSeachEx() throws Exception{
        java.util.Calendar fromCal = java.util.Calendar.getInstance();
        fromCal.add(java.util.Calendar.MINUTE, 5);
        java.util.Calendar toCal = java.util.Calendar.getInstance();
        toCal.add(java.util.Calendar.MINUTE, 65);

        //Simple case
        String keyword = "you are search able" ;
        EventQuery query = new UnifiedQuery() ;
        query.setText(keyword) ;
        query.setOrderType(Utils.ORDER_TYPE_ASCENDING);
        query.setOrderBy(new String[]{Utils.ORDERBY_TITLE});
        Collection<String> params = new ArrayList<String>();

        String john = "john" ;
        login(username) ;
        Calendar cal = new Calendar() ;
        cal.setName("root calendar");
        calendarService_.saveUserCalendar(username, cal, true);

        Calendar johnCalendar = new Calendar();
        johnCalendar.setName("john calendar");
        calendarService_.saveUserCalendar(john, johnCalendar, true);

        EventCategory eventCategory = new EventCategory() ;
        eventCategory.setName("Meeting");
        calendarService_.saveEventCategory(username, eventCategory, true);

        EventCategory johnEventCategory = new EventCategory();
        johnEventCategory.setName("Call to boss");
        calendarService_.saveEventCategory(john, johnEventCategory, true);

        CalendarEvent calEvent = new CalendarEvent();
        calEvent.setFromDateTime(fromCal.getTime());
        calEvent.setToDateTime(toCal.getTime());
        calEvent.setSummary("how do you do root");
        calEvent.setDescription("you are a search able event");
        calEvent.setCalendarId(cal.getId());
        calEvent.setEventCategoryId(eventCategory.getId());
        calEvent.setPrivate(false);
        calendarService_.saveUserEvent(username, cal.getId(),calEvent, true);

        CalendarEvent calEvent2 = new CalendarEvent() ;
        calEvent2.setFromDateTime(fromCal.getTime());
        calEvent2.setToDateTime(toCal.getTime());
        calEvent2.setSummary("are you here?");
        calEvent2.setDescription("I am not search able event with john");
        calEvent2.setCalendarId(cal.getId());
        calEvent2.setEventCategoryId(eventCategory.getId());
        calEvent2.setPrivate(true);
        calendarService_.saveUserEvent(username, cal.getId(), calEvent2, true);

        CalendarEvent task = new CalendarEvent() ;
        task.setEventType(CalendarEvent.TYPE_TASK);
        task.setFromDateTime(fromCal.getTime());
        task.setToDateTime(toCal.getTime());
        task.setSummary("are you here?");
        task.setDescription("I am search able task with john");
        task.setCalendarId(cal.getId());
        task.setEventCategoryId(eventCategory.getId());
        calendarService_.saveUserEvent(username, cal.getId(), task, true);

        cal.setViewPermission(new String[]{john});
        calendarService_.saveUserCalendar(username, cal, false);

        calendarService_.shareCalendar(username, cal.getId(), Arrays.asList(new String[]{john}));

        CalendarEvent johnEvent = new CalendarEvent() ;
        String[] ids = new String[]{cal.getId()};
        Collection<SearchResult> result = unifiedSearchService_.search(null, query.getText(), params, 0, 10, query.getOrderBy()[0] , query.getOrderType());
        assertNotNull(result) ;
        assertEquals(3, result.size());

        login(john);
        result = unifiedSearchService_.search(null, query.getText(), params, 0, 10, query.getOrderBy()[0] , query.getOrderType());
        assertEquals(3, result.size());

        // Clean up data
        calendarService_.removeUserEvent(username, ids[0], calEvent.getId());
        calendarService_.removeUserEvent(username, ids[0], calEvent2.getId());
        calendarService_.removeUserEvent(john, johnCalendar.getId(), johnEvent.getId());
        calendarService_.removeEventCategory(username, eventCategory.getId());
        calendarService_.removeEventCategory(john, johnEventCategory.getId());
        calendarService_.removeSharedCalendar(john, cal.getId());
        assertNotNull(calendarService_.removeUserCalendar(username, ids[0]));
        assertNotNull(calendarService_.removeUserCalendar(john, johnCalendar.getId()));

    }

    public void testUnifiedSeachAllWord() throws Exception {
        login(username) ;
        Calendar cal = new Calendar() ;
        cal.setName("root calendar");
        calendarService_.saveUserCalendar(username, cal, true);

        EventCategory eventCategory = new EventCategory() ;
        eventCategory.setName("Meeting");
        calendarService_.saveEventCategory(username, eventCategory, true);

        CalendarEvent calEvent = new CalendarEvent();
        java.util.Calendar fromCal = java.util.Calendar.getInstance();
        fromCal.add(java.util.Calendar.MINUTE, 1);
        java.util.Calendar toCal = java.util.Calendar.getInstance();
        toCal.add(java.util.Calendar.HOUR, 1);
        toCal.add(java.util.Calendar.MINUTE, 1);
        calEvent.setFromDateTime(fromCal.getTime());
        calEvent.setToDateTime(toCal.getTime());

        calEvent.setSummary("abcde fghik");
        calEvent.setDescription("I am a search able event");
        calEvent.setCalendarId(cal.getId());
        calEvent.setEventCategoryId(eventCategory.getId());
        calendarService_.saveUserEvent(username, cal.getId(),calEvent, true);

        CalendarEvent calEvent2 = new CalendarEvent() ;
        calEvent2.setSummary("abcde");
        calEvent2.setCalendarId(cal.getId());
        calEvent2.setEventCategoryId(eventCategory.getId());
        calEvent2.setFromDateTime(fromCal.getTime());
        calEvent2.setToDateTime(toCal.getTime());
        calendarService_.saveUserEvent(username, cal.getId(), calEvent2, true);

        CalendarEvent task = new CalendarEvent() ;
        task.setEventType(CalendarEvent.TYPE_TASK);
        task.setSummary("fghik");
        task.setCalendarId(cal.getId());
        task.setEventCategoryId(eventCategory.getId());
        calendarService_.saveUserEvent(username, cal.getId(), task, true);

        //Simple case
        String keyword = "abcde fghik" ;
        EventQuery query = new UnifiedQuery() ;
        query.setText(keyword) ;
        Collection<String> params = new ArrayList<String>();
        query.setOrderType(Utils.ORDER_TYPE_ASCENDING);
        query.setOrderBy(new String[]{Utils.ORDERBY_TITLE});
        Collection<SearchResult> result = unifiedSearchService_.search(null, query.getText(), params, 0, 10, query.getOrderBy()[0] , query.getOrderType());
        assertNotNull(result) ;
        assertEquals(result.size(), 1);
        assertEquals(keyword, result.iterator().next().getTitle());

        // Clean up data
        String[] ids = new String[]{cal.getId()};
        calendarService_.removeUserEvent(username, ids[0], calEvent.getId());
        calendarService_.removeUserEvent(username, ids[0], calEvent2.getId());
        calendarService_.removeEventCategory(username, eventCategory.getId());
        assertNotNull(calendarService_.removeUserCalendar(username, ids[0]));
    }

    /*
   * Utils for test
   */

    public void testSearchSpecialCharacter() throws Exception {
        login(username) ;
        Calendar cal = new Calendar();
        cal.setName("CalendarName");
        cal.setDescription("CalendarDescription");
        calendarService_.saveUserCalendar(username, cal, true);

        String keyword = "Have a%'\\_-\". meeting" ;
        CalendarEvent calEvent = new CalendarEvent();
        calEvent.setSummary(keyword);
        calEvent.setCalendarId(cal.getId());
        java.util.Calendar fromCal = java.util.Calendar.getInstance();
        java.util.Calendar toCal = java.util.Calendar.getInstance();
        toCal.add(java.util.Calendar.HOUR, 1);
        calEvent.setFromDateTime(fromCal.getTime());
        calEvent.setToDateTime(toCal.getTime());
        calendarService_.saveUserEvent(username, cal.getId(), calEvent, true);

        EventQuery query = new EventQuery();
        query.setText(keyword);

        query.setQueryType(Query.XPATH);
        assertEquals(1, calendarService_.searchEvent(username, query, new String[] {}).getAll().size());
        assertEquals(1, calendarService_.getEvents(username, query, new String[] {}).size());

        query.setQueryType(Query.SQL);
        assertEquals(1, calendarService_.searchEvent(username, query, new String[] {}).getAll().size());
        assertEquals(1, calendarService_.getEvents(username, query, new String[] {}).size());


        EventQuery uQuery = new UnifiedQuery();
        uQuery.setText(keyword) ;
        Collection<String> params = new ArrayList<String>();
        uQuery.setOrderType(Utils.ORDER_TYPE_ASCENDING);
        uQuery.setOrderBy(new String[]{Utils.ORDERBY_TITLE});
        Collection<SearchResult> result = unifiedSearchService_.search(null, uQuery.getText(), params, 0, 10, uQuery.getOrderBy()[0] , uQuery.getOrderType());
        assertNotNull(result);
        assertEquals(1, result.size());

        calendarService_.removeUserEvent(username, cal.getId(), calEvent.getId());
        calendarService_.removeUserCalendar(username, cal.getId());
    }

    public void testMultiThreadSearch() throws Exception {
        login(username) ;
        String keyword = "Have a meeting" ;

        List<Calendar> calendars = new LinkedList<Calendar>();
        for (int i = 0; i < 100; i++) {
            Calendar cal = new Calendar();
            cal.setName("CalendarName" +  i);
            calendarService_.saveUserCalendar(username, cal, true);
            calendars.add(cal);

            CalendarEvent calEvent = createCalendarEventInstance(keyword);
            calendarService_.saveUserEvent(username, cal.getId(), calEvent, true);
        }

        final EventQuery uQuery = new UnifiedQuery();
        uQuery.setText(keyword) ;
        final Collection<String> params = new ArrayList<String>();
        uQuery.setOrderType(Utils.ORDER_TYPE_ASCENDING);
        uQuery.setOrderBy(new String[]{Utils.ORDERBY_TITLE});

        //Workaround to make sure sharedCalendar JCR node has been created
        //It can't be created in multithreading
        unifiedSearchService_.search(null, uQuery.getText(), params, 0, -1, uQuery.getOrderBy()[0] , uQuery.getOrderType());

        final AtomicBoolean fail = new AtomicBoolean(false);
        final CountDownLatch wait = new CountDownLatch(1);
        Runnable runner = new Runnable() {
            @Override
            public void run() {
                try {
                    wait.await();
                } catch (InterruptedException e) {
                }
                begin();
                login(username) ;
                Collection<SearchResult> result = unifiedSearchService_.search(null, uQuery.getText(), params, 0, -1,
                        uQuery.getOrderBy()[0] , uQuery.getOrderType());
                if (result == null || result.size() < 100) {
                    fail.set(true);
                }
                end();
            }
        };

        List<Thread> threads = new LinkedList<Thread>();
        for (int i = 0; i < 20; i++) {
            Thread t = new Thread(runner);
            t.start();
            threads.add(t);
        }
        wait.countDown();

        //wait for all threads complete
        for (Thread t : threads) {
            t.join();
        }

        for (Calendar cal : calendars) {
            calendarService_.removeUserCalendar(username, cal.getId());
        }
        assertFalse(fail.get());
    }

    //TODO: @nttuyen consider move it to common
    private Router loadConfiguration(String path) throws IOException {
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

    private void checkFieldsValueWithType(String calName, CalendarEvent calEvent, CalendarSearchResult item){
        checkFieldsValueWithType(calName,  calEvent,  (SearchResult)item);
        if(CalendarEvent.TYPE_EVENT.equals(calEvent.getEventType())){
            assertEquals(item.getFromDateTime(), calEvent.getFromDateTime().getTime());
            assertNull(item.getImageUrl());
            assertEquals(Utils.EVENT_ICON_URL, item.getImageUrl());
        } else if(CalendarEvent.TYPE_TASK.equals(calEvent.getEventType())){
            assertEquals(0,item.getFromDateTime());
            assertNotNull(item.getImageUrl());
            assertEquals(Utils.TASK_ICON_URL, item.getImageUrl());
        }
        assertNotNull(item.getTimeZoneName());
    }
    private void checkFieldsValueWithType(String calName, CalendarEvent calEvent, SearchResult item){
        assertEquals(calEvent.getSummary(), item.getTitle());
        if(CalendarEvent.TYPE_EVENT.equals(calEvent.getEventType())){
            if(calEvent.getLocation() != null) assertEquals(calName + Utils.SPACE+ Utils.MINUS + Utils.SPACE+ df.format(calEvent.getFromDateTime())+ Utils.SPACE+Utils.MINUS+ Utils.SPACE+calEvent.getLocation(), item.getDetail()) ;
        } else {
            assertEquals(calName + Utils.SPACE +Utils.MINUS+ Utils.SPACE + Utils.DUE_FOR + df.format(calEvent.getToDateTime()), item.getDetail()) ;
        }
        SimpleDateFormat tempFm = new SimpleDateFormat("MM/dd/yyyy hh");
        assertEquals(tempFm.format(new Date()), tempFm.format(new Date(item.getDate())));
        assertEquals(true, item.getRelevancy() > 0);
        //case could not init url
        assertEquals(Utils.NONE_NAGVIGATION, item.getUrl());
        StringBuffer sb = new StringBuffer(Utils.EMPTY_STR) ;
        if(calEvent.getDescription() != null) sb.append(calEvent.getDescription());
        assertEquals(sb.toString(), item.getExcerpt());
    }

    private void checkFields(SearchResult item) {
        assertNotNull(item.getTitle()) ;
        assertNotNull(item.getExcerpt()) ;
        assertNotNull(item.getDetail()) ;
        assertNull(item.getImageUrl());
        assertNotNull(item.getUrl());
        assertEquals(true, item.getDate() > 0);
    }
    private void checkFields(CalendarSearchResult item) {
        checkFields((SearchResult)(item));
        assertEquals(item.getDataType(), CalendarEvent.TYPE_EVENT);
        if(CalendarEvent.TYPE_EVENT.equals(item.getDataType())){
            assertNull(item.getImageUrl());
            assertNull(item.getTaskStatus());
        } else if (CalendarEvent.TYPE_TASK.equals(item.getDataType())){
            assertEquals(Utils.TASK_ICON_URL, item.getImageUrl());
            assertNotNull(item.getTaskStatus());
        }
    }
}
