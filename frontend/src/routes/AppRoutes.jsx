import { lazy, Suspense } from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';

import AuthLayout      from '../layouts/AuthLayout.jsx';
import AppLayout       from '../layouts/AppLayout.jsx';
import ProtectedRoute  from '../components/ProtectedRoute.jsx';
import PublicOnlyRoute from '../components/PublicOnlyRoute.jsx';
import PageLoader      from '../components/PageLoader.jsx';
import { ROUTES }      from '../utils/constants.js';

// ─── Lazy pages for code-splitting ───────────────────
const Login          = lazy(() => import('../pages/Login.jsx'));
const Register       = lazy(() => import('../pages/Register.jsx'));
const ForgotPassword = lazy(() => import('../pages/ForgotPassword.jsx'));
const AuthAction     = lazy(() => import('../pages/AuthAction.jsx'));
const Dashboard      = lazy(() => import('../pages/Dashboard.jsx'));
const Automations    = lazy(() => import('../pages/Automations.jsx'));
const AutomationBuilder = lazy(() => import('../pages/AutomationBuilder.jsx'));
const Contacts       = lazy(() => import('../pages/Contacts.jsx'));
const Settings       = lazy(() => import('../pages/Settings.jsx'));
const ActivityLogs   = lazy(() => import('../pages/ActivityLogs.jsx'));
const FailedJobs     = lazy(() => import('../pages/FailedJobs.jsx'));
const InstagramCallback = lazy(() => import('../pages/InstagramCallback.jsx'));
const NotFound       = lazy(() => import('../pages/NotFound.jsx'));

const AppRoutes = () => {
  return (
    <Suspense fallback={<PageLoader />}>
      <Routes>
        {/* Password-reset / email-action handler — public, works whether or
            not the user is signed in (they arrive from an email link). */}
        <Route path="/auth/action" element={<AuthAction />} />

        {/* Public auth routes — bounce signed-in users to the dashboard */}
        <Route element={<PublicOnlyRoute />}>
          <Route element={<AuthLayout />}>
            <Route path={ROUTES.LOGIN}           element={<Login />} />
            <Route path={ROUTES.REGISTER}        element={<Register />} />
            <Route path={ROUTES.FORGOT_PASSWORD} element={<ForgotPassword />} />
          </Route>
        </Route>

        {/* Protected app — single shell wrapping every authenticated page */}
        <Route element={<ProtectedRoute />}>
          {/* Standalone protected pages (no sidebar/topbar chrome) */}
          <Route path={ROUTES.INSTAGRAM_CALLBACK} element={<InstagramCallback />} />
          <Route element={<AppLayout />}>
            <Route
              index
              element={<Navigate to={ROUTES.DASHBOARD} replace />}
            />
            <Route path={ROUTES.DASHBOARD}        element={<Dashboard />} />
            <Route path={ROUTES.AUTOMATIONS}      element={<Automations />} />
            <Route path={ROUTES.AUTOMATION_NEW}   element={<AutomationBuilder />} />
            <Route path={ROUTES.AUTOMATION_EDIT}  element={<AutomationBuilder />} />
            <Route path={ROUTES.CONTACTS}         element={<Contacts />} />
            <Route path={ROUTES.LOGS}             element={<ActivityLogs />} />
            <Route path={ROUTES.FAILED_JOBS}      element={<FailedJobs />} />
            <Route path={ROUTES.SETTINGS}         element={<Settings />} />
          </Route>
        </Route>

        {/* 404 */}
        <Route path="*" element={<NotFound />} />
      </Routes>
    </Suspense>
  );
};

export default AppRoutes;