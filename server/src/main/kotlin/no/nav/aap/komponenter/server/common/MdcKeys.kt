package no.nav.aap.komponenter.server.common

@Suppress("MayBeConstant")
public object MdcKeys {
    public val CallId: String = "callId"
    public val User: String = "x_user"
    public val Method: String = "x_method"
    public val InboundUri: String = "x_inbound_uri"
}
