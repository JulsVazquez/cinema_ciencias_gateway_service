package mx.unam.fciencias.cinema_ciencias.gateway_service.cinema_ciencias_gateway_service

import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.Collections
import java.util.Enumeration
import javax.crypto.SecretKey

private const val USER_ID_HEADER = "X-User-Id"
private const val USERNAME_HEADER = "X-User-Name"
private const val ROLES_HEADER = "X-User-Roles"
private const val GATEWAY_SECRET_HEADER = "X-Gateway-Secret"

/** Headers de confianza: solo el Gateway puede ponerlos, nunca el cliente. */
private val TRUSTED_HEADERS = setOf(USER_ID_HEADER, USERNAME_HEADER, ROLES_HEADER, GATEWAY_SECRET_HEADER)

/**
 * Implementa el patrón "Trust the Gateway".
 *
 * Es el único punto del sistema que valida la firma y la expiración del JWT. Si el token es
 * válido, reenvía la identidad a los microservicios en los headers `X-User-*` junto con un
 * secreto compartido. Los microservicios solo leen esos headers: no calculan firmas ni
 * consultan usuarios.
 *
 * Rutas públicas (no requieren token): todo lo que cuelga de `/api/v1/cinema/auth/` (login y registro) y
 * `POST /api/v1/cinema/user` (registro).
 */
@Component
class JwtTrustFilter(
    @Value("\${cinema.security.jwtSecret}") jwtSecret: String,
    @Value("\${cinema.security.gatewaySecret}") private val gatewaySecret: String
) : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(JwtTrustFilter::class.java)
    private val key: SecretKey = Keys.hmacShaKeyFor(jwtSecret.toByteArray(Charsets.UTF_8))

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        // Aunque la ruta sea pública, jamás se deja pasar una identidad declarada por el cliente.
        if (isPublic(request)) {
            filterChain.doFilter(TrustedHeadersRequest(request, emptyMap()), response)
            return
        }

        val claims = bearerToken(request)?.let { parseClaims(it) }
        val userId = (claims?.get("userId") as? Number)?.toLong()
        val username = claims?.subject

        if (claims == null || userId == null || username.isNullOrBlank()) {
            reject(response)
            return
        }

        val roles = (claims["roles"] as? Collection<*>)?.joinToString(",") ?: ""
        val trusted = mapOf(
            USER_ID_HEADER to userId.toString(),
            USERNAME_HEADER to username,
            ROLES_HEADER to roles,
            GATEWAY_SECRET_HEADER to gatewaySecret
        )
        filterChain.doFilter(TrustedHeadersRequest(request, trusted), response)
    }

    private fun isPublic(request: HttpServletRequest): Boolean {
        val path = request.requestURI
        if (path.startsWith("/api/v1/cinema/auth/")) return true
        return request.method == "POST" && path.trimEnd('/') == "/api/v1/cinema/user"
    }

    private fun bearerToken(request: HttpServletRequest): String? {
        val header = request.getHeader("Authorization") ?: return null
        return if (header.startsWith("Bearer ")) header.substring(7).trim() else null
    }

    private fun parseClaims(token: String): Claims? =
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload
        } catch (e: JwtException) {
            log.debug("JWT rechazado: {}", e.message)
            null
        } catch (e: IllegalArgumentException) {
            log.debug("JWT vacío o mal formado: {}", e.message)
            null
        }

    private fun reject(response: HttpServletResponse) {
        response.status = HttpStatus.UNAUTHORIZED.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()
        response.writer.write(
            """{"status":401,"error":"Unauthorized","message":"Token JWT ausente, inválido o expirado"}"""
        )
    }
}

/**
 * Oculta los headers de confianza que haya mandado el cliente y expone, en su lugar, solo los
 * que el Gateway verificó. Como el DispatcherServlet recibe este wrapper, el proxy hacia los
 * microservicios reenvía exactamente estos headers.
 */
private class TrustedHeadersRequest(
    request: HttpServletRequest,
    private val trustedHeaders: Map<String, String>
) : HttpServletRequestWrapper(request) {

    override fun getHeader(name: String): String? =
        trustedValue(name) ?: if (isManaged(name)) null else super.getHeader(name)

    override fun getHeaders(name: String): Enumeration<String> {
        val trusted = trustedValue(name)
        return when {
            trusted != null -> Collections.enumeration(listOf(trusted))
            isManaged(name) -> Collections.emptyEnumeration<String>()
            else -> super.getHeaders(name)
        }
    }

    override fun getHeaderNames(): Enumeration<String> {
        val original = (super.getHeaderNames() ?: Collections.emptyEnumeration<String>()).toList()
        return Collections.enumeration(original.filterNot { isManaged(it) } + trustedHeaders.keys)
    }

    private fun trustedValue(name: String): String? =
        trustedHeaders.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value

    private fun isManaged(name: String): Boolean =
        TRUSTED_HEADERS.any { it.equals(name, ignoreCase = true) }
}
