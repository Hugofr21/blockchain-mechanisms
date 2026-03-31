import React from 'react';
import { Navigate, Outlet } from 'react-router';
import { useAuth } from '../infrastructure/hooks/useAuth';

export const PrivateRoute = () => {
  const { isAuthenticated } = useAuth();

  if (!isAuthenticated) {
    return <Navigate to="/" replace />;
  }

  return <Outlet />;
};