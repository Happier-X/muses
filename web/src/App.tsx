import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import Layout from './components/Layout';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<div>Login page coming soon</div>} />
        <Route path="/" element={<Layout />}>
          <Route index element={<div>Library coming soon</div>} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}

export default App;
