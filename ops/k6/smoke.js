import http from 'k6/http'
import { check, fail, sleep } from 'k6'
import { SharedArray } from 'k6/data'

const baseUrl = (__ENV.BASE_URL || 'http://localhost:8088').replace(/\/$/, '')
const tenantSlug = __ENV.TENANT_SLUG
const email = __ENV.CRMIX_EMAIL
const password = __ENV.CRMIX_PASSWORD

export const options = {
  scenarios: {
    api_smoke: {
      executor: 'constant-arrival-rate',
      rate: 50,
      timeUnit: '1s',
      duration: __ENV.DURATION || '2m',
      preAllocatedVUs: 40,
      maxVUs: 150,
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<750', 'p(99)<1500'],
    checks: ['rate>0.99'],
  },
}

export function setup() {
  if (!tenantSlug || !email || !password) {
    fail('TENANT_SLUG, CRMIX_EMAIL and CRMIX_PASSWORD are required')
  }

  const response = http.post(`${baseUrl}/api/v1/auth/login`, JSON.stringify({ tenantSlug, email, password }), {
    headers: { 'Content-Type': 'application/json' },
    tags: { endpoint: 'login' },
  })
  check(response, { 'login succeeds': (r) => r.status === 200 }) || fail(`login failed: ${response.status}`)
  return { accessToken: response.json('accessToken') }
}

const paths = new SharedArray('read endpoints', () => [
  '/api/v1/clients?page=0&size=20',
  '/api/v1/employees?page=0&size=20',
  '/api/v1/services?page=0&size=20',
  '/api/v1/appointments?page=0&size=20',
])

export default function (data) {
  const index = Math.floor(Math.random() * paths.length)
  const path = paths[index]
  const response = http.get(`${baseUrl}${path}`, {
    headers: { Authorization: `Bearer ${data.accessToken}` },
    tags: { endpoint: path.split('?')[0] },
  })

  check(response, {
    'business endpoint is successful': (r) => r.status === 200,
    'response is json': (r) => (r.headers['Content-Type'] || '').includes('application/json'),
  })
  sleep(0.01)
}

export function teardown() {
  const health = http.get(`${baseUrl}/actuator/health`, { tags: { endpoint: 'health' } })
  check(health, { 'backend healthy after load': (r) => r.status === 200 && r.json('status') === 'UP' })
}
