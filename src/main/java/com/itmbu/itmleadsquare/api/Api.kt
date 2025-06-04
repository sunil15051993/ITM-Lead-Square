package com.itmbu.itmleadsquare.api

object Api {
    const val BASE_URL = "http://am.elctech.in/itm_crm/api/api.php?apicall="
    const val LOGIN = BASE_URL + "login"
    const val GET_STAFF_LEAD = BASE_URL + "get_staff_short_leads"
    const val VIEW_STAFF_LEAD = BASE_URL + "get_staff_leads_byleadid"
    const val STAFF_LEAD_POST = BASE_URL + "save_call_record"
}